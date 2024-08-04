package io.github.rxcats.aws.dynamodb.extensions

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
private fun KType.toEnhancedType(schemaCache: MetaTableSchemaCache): EnhancedType<out Any> {
    return when (val clazz = classifier as KClass<Any>) {
        List::class -> {
            val listType = arguments.first().type!!.toEnhancedType(schemaCache)
            EnhancedType.listOf(listType)
        }

        Set::class -> {
            val setType = arguments.first().type!!.toEnhancedType(schemaCache)
            EnhancedType.setOf(setType)
        }

        Map::class -> {
            val (key, value) = arguments.map { it.type!!.toEnhancedType(schemaCache) }
            EnhancedType.mapOf(key, value)
        }

        else -> {
            if (clazz.isData) {
                EnhancedType.documentOf(clazz.java, recursiveDataClassTableSchema(clazz, schemaCache)) {
                    if (clazz.findAnnotation<DynamoDbKtPreserveEmptyObject>() != null) {
                        it.preserveEmptyObject(true)
                    }
                }
            } else {
                EnhancedType.of(javaType)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <Attr : Any?> initConverter(clazz: KClass<out AttributeConverter<Attr>>): AttributeConverter<Attr> {
    clazz.constructors.firstOrNull { it.visibility == KVisibility.PUBLIC }
        ?.let { return it.call() }

    return clazz.staticFunctions
        .filter { it.name == "create" }
        .filter { it.visibility == KVisibility.PUBLIC }
        .first { it.parameters.isEmpty() }
        .call() as AttributeConverter<Attr>
}

private fun KProperty1<out Any, *>.tags() = buildList {
    for (annotation in getter.annotations) {
        when (annotation) {
            is DynamoDbPartitionKey -> add(StaticAttributeTags.primaryPartitionKey())
            is DynamoDbSortKey -> add(StaticAttributeTags.primarySortKey())
            is DynamoDbSecondaryPartitionKey -> add(StaticAttributeTags.secondaryPartitionKey(annotation.indexNames.toList()))
            is DynamoDbSecondarySortKey -> add(StaticAttributeTags.secondarySortKey(annotation.indexNames.toList()))
            is DynamoDbUpdateBehavior -> add(StaticAttributeTags.updateBehavior(annotation.value))
            else -> {
                val tagAnnotation = annotation.annotationClass.findAnnotation<BeanTableSchemaAttributeTag>() ?: continue
                for (fn in tagAnnotation.value.staticFunctions) {
                    for (parameter in fn.parameters) {
                        if (parameter.type.jvmErasure == annotation.annotationClass) {
                            add(fn.call(annotation) as StaticAttributeTag)
                            break
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <Table : Any, Attr : Any?> KProperty1<Table, Attr>.toImmutableDataClassAttribute(
    dataClass: KClass<Table>,
    schemaCache: MetaTableSchemaCache
): ImmutableAttribute<Table, ImmutableDataClassBuilder, Attr> {
    val converter = findAnnotation<DynamoDbKtConvertedBy>()
        ?.value
        ?.let { it as KClass<AttributeConverter<Attr>> }
        ?.let { initConverter(it) }
        ?: AttributeConverterProvider.defaultProvider().converterFor(returnType.toEnhancedType(schemaCache)) as AttributeConverter<Attr>

    return ImmutableAttribute
        .builder(
            EnhancedType.of(dataClass.java),
            EnhancedType.of(ImmutableDataClassBuilder::class.java),
            converter.type()
        )
        .name(findAnnotation<DynamoDbKtAttribute>()?.name ?: name)
        .getter(::get)
        .setter { builder, value -> builder[name] = value }
        .attributeConverter(converter)
        .tags(tags())
        .build()
}
