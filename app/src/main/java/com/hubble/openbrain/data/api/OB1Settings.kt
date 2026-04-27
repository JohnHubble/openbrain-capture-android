package com.hubble.openbrain.data.api

import kotlinx.coroutines.flow.Flow

interface OB1Settings {
    val endpoint: Flow<String>
    val accessKey: Flow<String>
}
