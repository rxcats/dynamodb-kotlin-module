package io.github.rxcats.aws.dynamodb.query

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse

class DynamoDbTableOperations(
    private val dynamoDbClient: DynamoDbClient
) {

    fun describeTable(tableName: String): DescribeTableResponse {
        return dynamoDbClient.describeTable {
            it.tableName(tableName)
        }
    }

    fun createTable(param: DynamoDbCreateTableParam): CreateTableResponse {
        require(param.tableName.isNotBlank()) { "tableName must not be blank" }
        require(param.partitionKeyName.isNotBlank()) { "partitionKeyName must not be blank" }

        val keySchemaList = mutableListOf<KeySchemaElement>()
        val attributeDefinitionList = mutableListOf<AttributeDefinition>()

        keySchemaList += KeySchemaElement.builder()
            .attributeName(param.partitionKeyName)
            .keyType(KeyType.HASH)
            .build()

        attributeDefinitionList += AttributeDefinition.builder()
            .attributeName(param.partitionKeyName)
            .attributeType(ScalarAttributeType.S)
            .build()

        if (param.sortKeyName.isNotBlank()) {
            keySchemaList += KeySchemaElement.builder()
                .attributeName(param.sortKeyName)
                .keyType(KeyType.RANGE)
                .build()

            attributeDefinitionList += AttributeDefinition.builder()
                .attributeName(param.sortKeyName)
                .attributeType(ScalarAttributeType.S)
                .build()
        }

        return dynamoDbClient.createTable { req ->
            req.tableName(param.tableName)
                .keySchema(keySchemaList)
                .attributeDefinitions(attributeDefinitionList)
                .provisionedThroughput { provision ->
                    provision.readCapacityUnits(param.readCapacityUnits)
                    provision.writeCapacityUnits(param.writeCapacityUnits)
                }
        }
    }

    fun updateTableProvision(param: DynamoDbUpdateTableParam): UpdateTableResponse {
        return dynamoDbClient.updateTable { req ->
            req.tableName(param.tableName)
                .provisionedThroughput { provision ->
                    provision.readCapacityUnits(param.readCapacityUnits)
                    provision.writeCapacityUnits(param.writeCapacityUnits)
                }
        }
    }

    fun deleteTable(tableName: String): DeleteTableResponse {
        require(tableName.isNotBlank()) { "tableName must not be blank" }

        return dynamoDbClient.deleteTable { req ->
            req.tableName(tableName)
        }
    }

}
