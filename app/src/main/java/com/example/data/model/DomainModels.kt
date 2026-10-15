package com.example.data.model

data class ServerNode(
    val id: String,
    val name: String,
    val region: String,
    val ip: String,
    val pingMs: Int,
    val loadPercentage: Int,
    val isOnline: Boolean,
    val activeConnections: Int,
    val maxConnections: Int
)

data class SprixQuestion(
    val id: String,
    val subject: Int, // 1, 2, or 3
    val chapter: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val difficulty: String = "Normal"
)

data class QureoStage(
    val stageNumber: Int,
    val chapter: String,
    val title: String,
    val objective: String,
    val solutionBlocks: List<String>,
    val pythonCode: String,
    val difficulty: String
)

data class WisoQuestion(
    val id: String,
    val topic: String,
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val rationale: String
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val durationDays: Int,
    val priceUsd: Double,
    val features: List<String>,
    val badge: String = ""
)
