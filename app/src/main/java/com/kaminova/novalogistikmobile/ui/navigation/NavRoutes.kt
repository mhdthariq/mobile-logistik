package com.kaminova.novalogistikmobile.ui.navigation

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object Main : NavRoute("main")
    data object Capture : NavRoute("capture")

    data object TransportDetail : NavRoute("transport_detail/{id}") {
        fun createRoute(id: Long) = "transport_detail/$id"
    }

    data object Expenses : NavRoute("expenses/{id}") {
        fun createRoute(id: Long) = "expenses/$id"
    }

    data object Drops : NavRoute("drops/{id}") {
        fun createRoute(id: Long) = "drops/$id"
    }
}

enum class MainTab(val title: String) {
    HOME("Beranda"),
    HISTORY("Riwayat"),
    PROFILE("Profil")
}
