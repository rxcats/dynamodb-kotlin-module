package io.github.rxcats.aws.dynamodb.converter

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class CompressionAttributeConverter : AttributeConverter<ByteArray> {
    override fun transformFrom(input: ByteArray): AttributeValue {
        return AttributeValue.builder()
            .b(SdkBytes.fromByteArray(compress(input)))
            .build()
    }

    override fun transformTo(input: AttributeValue?): ByteArray {
        if (input == null) return byteArrayOf()
        return decompress(input.b().asByteArray())
    }

    override fun type(): EnhancedType<ByteArray> {
        return EnhancedType.of(ByteArray::class.java)
    }

    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.B
    }

    private fun compress(input: ByteArray): ByteArray {
        val def = Deflater()
        def.setLevel(Deflater.BEST_COMPRESSION)
        def.setInput(input)
        def.finish()

        return ByteArrayOutputStream().use { os ->
            val buffer = ByteArray(1024)

            while (!def.finished()) {
                val compressedSize = def.deflate(buffer)
                os.write(buffer, 0, compressedSize)
            }

            os.toByteArray()
        }
    }

    private fun decompress(input: ByteArray): ByteArray {
        val inf = Inflater()
        inf.setInput(input)

        return ByteArrayOutputStream().use { os ->
            val buffer = ByteArray(1024)

            while (!inf.finished()) {
                val decompressedSize = inf.inflate(buffer)
                os.write(buffer, 0, decompressedSize)
            }

            os.toByteArray()
        }
    }
}
