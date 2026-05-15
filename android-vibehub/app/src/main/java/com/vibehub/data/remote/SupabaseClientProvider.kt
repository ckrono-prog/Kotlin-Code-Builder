package com.vibehub.data.remote

import com.vibehub.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Provides the Supabase client singleton.
 * Injected via Hilt — never instantiate directly.
 */
object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl   = BuildConfig.SUPABASE_URL,
        supabaseKey   = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
