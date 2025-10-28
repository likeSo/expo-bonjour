//
//  ExpoBonjourModule.types.swift
//  ExpoBonjour
//
//  Created by Aron on 2025/10/28.
//

import ExpoModulesCore

struct PublishingOptions: Record {
    @Field var name: String = ""
    @Field var port: UInt16 = 80
    @Field var service: String = ""
    @Field var domain: String?
    @Field var txtRecord: [String: String]?
}
