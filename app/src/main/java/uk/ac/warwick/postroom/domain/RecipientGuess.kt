package uk.ac.warwick.postroom.domain

data class RecipientGuess (
    val value: String,
    val type: RecipientGuessType,
    val id: String
)

enum class RecipientGuessType {
    UniversityId, Room
}