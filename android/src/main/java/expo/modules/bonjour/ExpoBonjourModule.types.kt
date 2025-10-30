package expo.modules.bonjour

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class PublishingOptions: Record {
    @Field var name: String = ""
    @Field var port: Int = 80
    @Field var service: String = ""
    @Field var domain: String? = null
    @Field var txtRecord: Map<String, String>? = null
}
