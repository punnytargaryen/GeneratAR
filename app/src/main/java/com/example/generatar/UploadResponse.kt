package com.example.generatar

data class UploadResponse(
        val error: Boolean,
        val message: String,
        val image: String?
)