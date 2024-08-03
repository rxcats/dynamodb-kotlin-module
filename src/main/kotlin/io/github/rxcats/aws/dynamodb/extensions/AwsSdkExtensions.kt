package io.github.rxcats.aws.dynamodb.extensions

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

fun TableMetadata.createTableEnhancedRequestWithIndices(
    createProjection: (IndexMetadata) -> Projection
): CreateTableEnhancedRequest {
    val globalIndices = indices()
        .filter { it.partitionKey().get().name() != primaryPartitionKey() }
        .map { index ->
            EnhancedGlobalSecondaryIndex.builder()
                .indexName(index.name())
                .projection(createProjection(index))
                .build()
        }

    val localIndices = indices()
        .filter { it.partitionKey().get().name() == primaryPartitionKey() && it.name() != TableMetadata.primaryIndexName() }
        .map { index ->
            EnhancedLocalSecondaryIndex.builder()
                .indexName(index.name())
                .projection(createProjection(index))
                .build()
        }

    return CreateTableEnhancedRequest.builder().apply {
        if (globalIndices.isNotEmpty()) globalSecondaryIndices(globalIndices)
        if (localIndices.isNotEmpty()) localSecondaryIndices(localIndices)
    }.build()
}

private val defaultProjectionBuilder = { _: IndexMetadata ->
    Projection.builder().projectionType(ProjectionType.ALL).build()
}

fun <T : Any> DynamoDbTable<T>.createTableWithIndices(
    createProjection: (IndexMetadata) -> Projection = defaultProjectionBuilder
) {
    val request = tableSchema().tableMetadata().createTableEnhancedRequestWithIndices(createProjection)
    createTable(request)
}

private fun getTableName(entity: KClass<*>): String = entity.findAnnotation<DynamoDbKtTableName>()?.name ?: entity.simpleName ?: ""

@Suppress("UNCHECKED_CAST")
fun <T> DynamoDbEnhancedClient.tableOf(entity: KClass<*>, prefixTableName: String): DynamoDbTable<T> {
    val bean = DataClassTableSchema(entity)
    val entityTableName = getTableName(entity)
    val tableName = String.format("%s%s", prefixTableName, entityTableName)
    return this.table(tableName, bean) as DynamoDbTable<T>
}
