package com.billwerk.checkout

class SessionValidator {

    companion object {
        private const val sessionPattern = "(^(cs|pa)_[a-f0-9]{32}$)|(^mock_.*)"

        fun validateToken(token: String): Boolean {
            val pattern = Regex(sessionPattern)
            return pattern.matches(token)
        }
    }
}