package com.hubble.openbrain.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Long,
    val method: String,
    val params: ToolsCallParams,
)

@Serializable
data class ToolsCallParams(
    val name: String,
    val arguments: JsonObject,
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String? = null,
    val id: JsonElement? = null,
    val result: McpResult? = null,
    val error: JsonElement? = null,
)

@Serializable
data class McpResult(
    val content: List<McpContent>? = null,
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null,
)
