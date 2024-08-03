package io.github.rxcats.aws.dynamodb.query

import io.github.rxcats.aws.dynamodb.extensions.createTableWithIndices
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse
import software.amazon.awssdk.enhanced.dynamodb.model.Page
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import kotlin.properties.Delegates
import kotlin.reflect.KClass

abstract class AbstractDynamoDbRepository<T : Any> {
    protected var enhancedClient by Delegates.notNull<DynamoDbEnhancedClient>()
    protected var entityType by Delegates.notNull<KClass<T>>()
    protected var table by Delegates.notNull<DynamoDbTable<T>>()

    open val tableName: String
        get() = table.tableName()

    open val mappedTableResource: MappedTableResource<T>
        get() = table

    open fun createTable() {
        return table.createTableWithIndices()
    }

    open fun deleteTable() {
        table.deleteTable()
    }

    /**
     * Returns information about the table, including the current status of the table, when it was created, the primary
     * key schema, and any indexes on the table.
     *
     * @throws software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
     *  The operation tried to access a nonexistent table or index.
     */
    open fun describeTable(): DescribeTableEnhancedResponse {
        return table.describeTable()
    }

    open fun getItem(key: Key): T? {
        return table.getItem(key)
    }

    open fun getItem(keyItem: T): T? {
        return table.getItem(keyItem)
    }

    open fun transactionGetItem(key: Key): T? {
        val res = enhancedClient.transactGetItems { req ->
            req.addGetItem(table, key)
        }
        return res.firstOrNull()?.getItem(table)
    }

    open fun transactionGetItem(keyItem: T): T? {
        val res = enhancedClient.transactGetItems { req ->
            req.addGetItem(table, keyItem)
        }
        return res.firstOrNull()?.getItem(table)
    }

    open fun transactionGetItemsByKeys(keys: List<Key>): List<T?> {
        if (keys.isEmpty()) return emptyList()

        require(keys.size <= MAX_TRANSACTION_GET_ITEM_SIZE) { "keys must be less than or equal to $MAX_TRANSACTION_GET_ITEM_SIZE" }

        val res = enhancedClient.transactGetItems { req ->
            keys.forEach { key ->
                req.addGetItem(table, key)
            }
        }
        return res.map { it.getItem(table) }
    }

    open fun transactionGetItems(keyItems: List<T>): List<T?> {
        if (keyItems.isEmpty()) return emptyList()

        require(keyItems.size <= MAX_TRANSACTION_GET_ITEM_SIZE) { "keyItems must be less than or equal to $MAX_TRANSACTION_GET_ITEM_SIZE" }

        val res = enhancedClient.transactGetItems { req ->
            keyItems.forEach { item ->
                req.addGetItem(table, item)
            }
        }
        return res.map { it.getItem(table) }
    }

    open fun batchGetItems(keys: List<Key>): List<T> {
        if (keys.isEmpty()) return emptyList()

        require(keys.size <= MAX_BATCH_GET_ITEM_SIZE) { "keys must be less than or equal to $MAX_BATCH_GET_ITEM_SIZE" }

        val builder = ReadBatch.builder(entityType.java)
            .mappedTableResource(table)

        keys.forEach { key ->
            builder.addGetItem(key)
        }

        val result = enhancedClient.batchGetItem { req ->
            req.readBatches(builder.build())
        }

        return result.resultsForTable(table).toList()
    }

    open fun getPage(param: DynamoDbPageQueryParam): Page<T> {
        val builder = QueryEnhancedRequest.builder()
            .limit(param.limit)
            .scanIndexForward(param.sort.scanIndexForward)

        when (param.queryConditional) {
            DynamoDbQueryConditional.SORT_BEGINS_WITH -> builder.queryConditional(QueryConditional.sortBeginsWith(param.key))
            DynamoDbQueryConditional.SORT_LESS_THAN -> builder.queryConditional(QueryConditional.sortLessThan(param.key))
            DynamoDbQueryConditional.SORT_GREATER_THAN -> builder.queryConditional(QueryConditional.sortGreaterThan(param.key))
            DynamoDbQueryConditional.KEY_EQUAL_TO -> builder.queryConditional(QueryConditional.keyEqualTo(param.key))
        }

        return table.query(builder.build()).first()
    }

    open fun save(item: T): T {
        return table.updateItem(item)
    }

    open fun transactionSave(item: T) {
        enhancedClient.transactWriteItems { req ->
            req.addPutItem(table, item)
        }
    }

    open fun batchSaveItems(items: List<T>) {
        if (items.isEmpty()) return

        require(items.size <= MAX_BATCH_SAVE_ITEM_SIZE) { "items must be less than or equal to $MAX_BATCH_SAVE_ITEM_SIZE" }

        val builder = WriteBatch.builder(entityType.java)
            .mappedTableResource(table)

        items.forEach { item ->
            builder.addPutItem(item)
        }

        enhancedClient.batchWriteItem { req -> req.writeBatches(builder.build()) }
    }

    open fun transactionSaveItems(items: List<T>) {
        if (items.isEmpty()) return

        require(items.size <= MAX_TRANSACTION_SAVE_ITEM_SIZE) { "items must be less than or equal to $MAX_TRANSACTION_SAVE_ITEM_SIZE" }

        enhancedClient.transactWriteItems { req ->
            items.forEach { item ->
                req.addPutItem(table, item)
            }
        }
    }

    open fun delete(key: Key): T {
        return table.deleteItem(key)
    }

    open fun delete(keyItem: T): T {
        return table.deleteItem(keyItem)
    }

    open fun transactionDelete(key: Key) {
        enhancedClient.transactWriteItems { req ->
            req.addDeleteItem(table, key)
        }
    }

    open fun transactionDelete(keyItem: T) {
        enhancedClient.transactWriteItems { req ->
            req.addDeleteItem(table, keyItem)
        }
    }

    open fun batchDeleteByKeys(keys: List<Key>) {
        if (keys.isEmpty()) return

        require(keys.size <= MAX_BATCH_DELETE_ITEM_SIZE) { "keys must be less than or equal to $MAX_BATCH_DELETE_ITEM_SIZE" }

        val builder = WriteBatch.builder(entityType.java)
            .mappedTableResource(table)

        keys.forEach { key ->
            builder.addDeleteItem(key)
        }

        enhancedClient.batchWriteItem { req -> req.writeBatches(builder.build()) }
    }

    open fun batchDelete(items: List<T>) {
        if (items.isEmpty()) return

        require(items.size <= MAX_BATCH_DELETE_ITEM_SIZE) { "items must be less than or equal to $MAX_BATCH_DELETE_ITEM_SIZE" }

        val builder = WriteBatch.builder(entityType.java)
            .mappedTableResource(table)

        items.forEach { entity ->
            builder.addDeleteItem(entity)
        }

        enhancedClient.batchWriteItem { req -> req.writeBatches(builder.build()) }
    }

    open fun transactionDeleteItemsByKeys(keys: List<Key>) {
        if (keys.isEmpty()) return

        require(keys.size <= MAX_TRANSACTION_DELETE_ITEM_SIZE) { "keys must be less than or equal to $MAX_TRANSACTION_DELETE_ITEM_SIZE" }

        enhancedClient.transactWriteItems { req ->
            keys.forEach { key ->
                req.addDeleteItem(table, key)
            }
        }
    }

    open fun transactionDeleteItems(items: List<T>) {
        if (items.isEmpty()) return

        require(items.size <= MAX_TRANSACTION_DELETE_ITEM_SIZE) { "items must be less than or equal to $MAX_TRANSACTION_DELETE_ITEM_SIZE" }

        enhancedClient.transactWriteItems { req ->
            items.forEach { item ->
                req.addDeleteItem(table, item)
            }
        }
    }

    companion object {
        private const val MAX_TRANSACTION_GET_ITEM_SIZE = 100
        private const val MAX_TRANSACTION_DELETE_ITEM_SIZE = 100
        private const val MAX_TRANSACTION_SAVE_ITEM_SIZE = 100

        private const val MAX_BATCH_GET_ITEM_SIZE = 100
        private const val MAX_BATCH_DELETE_ITEM_SIZE = 25
        private const val MAX_BATCH_SAVE_ITEM_SIZE = 25
    }
}
