package com.zephyrus.app.data.remote

import com.zephyrus.app.data.model.GitHubRelease
import retrofit2.http.GET

interface GitHubApiService {

    @GET("repos/marlobello/zephyrus/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
