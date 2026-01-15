package com.sid.civilq_1.SupabaseModule.kt

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage


object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://cpbxcbxjbjuijsplspgo.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNwYnhjYnhqYmp1aWpzcGxzcGdvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc3NzA5NjgsImV4cCI6MjA4MzM0Njk2OH0.c87NakFjVYCNsdJW2lfED6uDCjIF23U6KhjYbErCKVw"
    ) {
        install(Postgrest)
        install(Storage)
        install(Auth)  // Added for Supabase authentication
    }
}