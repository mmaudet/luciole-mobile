package fr.openllm.luciole.contact

data class ContactCard(
    val fullName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val company: String? = null,
    val jobTitle: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val website: String? = null,
    val address: String? = null,
    val note: String? = null,
) {
    fun displayName(): String =
        fullName?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(firstName, lastName).joinToString(" ").takeIf { it.isNotBlank() }
            ?: company.orEmpty()

    fun hasAnyField(): Boolean =
        displayName().isNotBlank()
            || company?.isNotBlank() == true
            || jobTitle?.isNotBlank() == true
            || phones.isNotEmpty()
            || emails.isNotEmpty()
            || website?.isNotBlank() == true
            || address?.isNotBlank() == true
            || note?.isNotBlank() == true
}
