package com.streamliners.timify.android.helper

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.BasicAuthentication
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.streamliners.timify.BuildConfig

class SheetsHelper(
    tokens: GoogleOAuthTokens
) {
    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val sheets = Sheets.Builder(
        transport, jsonFactory, tokens.toCredential()
    ).setApplicationName("Timify").build()

    fun createNewSheet(): String {
        val spreadsheet = Spreadsheet()
            .setProperties(
                SpreadsheetProperties()
                    .setTitle("Timify Data")
            )

        val result = sheets.spreadsheets().create(spreadsheet).execute()
        return result.spreadsheetId
    }

    private fun GoogleOAuthTokens.toCredential(): Credential {
        val tokenResponse = TokenResponse()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)

        return Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
            .setClientAuthentication(
                BasicAuthentication(
                    BuildConfig.serverId,
                    BuildConfig.serverSecret
                )
            )
            .build()
            .setFromTokenResponse(tokenResponse)
    }

}