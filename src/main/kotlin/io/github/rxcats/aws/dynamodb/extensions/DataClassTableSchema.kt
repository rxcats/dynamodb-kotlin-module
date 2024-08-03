package io.github.rxcats.aws.dynamodb.extensions

import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore
import java.util.Collections
import java.util.WeakHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

private val TABLE_SCHEMA_CACHE = Collections.synchronizedMap(WeakHashMap<KClass<*>, TableSchema<*>>())

@Suppress("UNCHECKED_CAST", "FunctionName")
fun <Item : Any> DataClassTableSchema(dataClass: KClass<Item>): TableSchema<Item> {
    return TABLE_SCHEMA_CACHE.computeIfAbsent(dataClass) { clz ->
        dataClassTableSchema(clz, MetaTableSchemaCache())
    } as TableSchema<Item>
}

@Suppress("UNCHECKED_CAST")
internal fun <Item : Any> dataClassTableSchema(dataClass: KClass<Item>, schemaCache: MetaTableSchemaCache): TableSchema<Item> {
    require(dataClass.isData) { "$dataClass must be a data class" }

    val props = dataClass.declaredMemberProperties
        .filterNot { it.getter.hasAnnotation<DynamoDbIgnore>() }

    require(props.size == dataClass.primaryConstructor!!.parameters.size) {
        "${dataClass.simpleName} properties must all be declared in the constructor"
    }

    val constructor = dataClass.primaryConstructor
        ?.takeIf { it.visibility == KVisibility.PUBLIC }
        ?: error("${dataClass.simpleName} must have a public primary constructor")

    val metaSchema = schemaCache.getOrCreate(dataClass.java)

    return StaticImmutableTableSchema.builder(dataClass.java, ImmutableDataClassBuilder::class.java)
        .newItemBuilder({ ImmutableDataClassBuilder(constructor) }, { it.build() as Item }).apply {
            for (prop in props) {
                if (prop.findAnnotation<DynamoDbKtFlatten>() != null) {
                    val schema = DataClassTableSchema(prop.returnType.classifier as KClass<Any>)
                    flatten(schema, prop.getter) { builder, value -> builder[prop.name] = value }
                } else {
                    addAttribute(prop.toImmutableDataClassAttribute(dataClass, schemaCache))
                }
            }
        }
        .build()
        .also { metaSchema.initialize(it) }
}

internal fun <Item : Any> recursiveDataClassTableSchema(dataClass: KClass<Item>, schemaCache: MetaTableSchemaCache): TableSchema<Item> {
    val metaTableSchema = schemaCache.get(dataClass.java)

    if (metaTableSchema.isPresent) {
        return if (metaTableSchema.get().isInitialized) {
            metaTableSchema.get().concreteTableSchema()
        } else metaTableSchema.get()
    }

    return dataClassTableSchema(dataClass, schemaCache)
}
