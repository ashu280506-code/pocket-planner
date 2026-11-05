package com.ashu.pocketplanner.logic

/** Picks an emoji from the words in a reminder. First matching rule wins. */
object EmojiGuess {

    private val rules: List<Pair<List<String>, String>> = listOf(
        listOf("call", "phone", "ring", "facetime") to "📞",
        listOf("lab") to "🧪",
        listOf("exam", "test", "quiz", "midsem", "endsem", "compre", "viva") to "📝",
        listOf("assignment", "homework", "report", "submit", "submission", "project", "ppt", "presentation") to "📄",
        listOf("study", "revise", "revision", "read", "lecture", "class", "tutorial", "notes") to "📚",
        listOf("code", "coding", "leetcode", "dsa", "debug", "deploy", "github", "hackathon") to "💻",
        listOf("gym", "workout", "exercise", "lift", "pushup") to "🏋️",
        listOf("run", "running", "jog") to "🏃",
        listOf("walk") to "🚶",
        listOf("yoga", "meditate", "meditation", "stretch") to "🧘",
        listOf("swim") to "🏊",
        listOf("cycle", "cycling") to "🚴",
        listOf("cricket") to "🏏",
        listOf("football", "match") to "⚽",
        listOf("badminton", "tennis") to "🏸",
        listOf("pay", "bill", "rent", "emi", "fees", "fee", "recharge", "bank", "money", "upi", "transfer") to "💸",
        listOf("buy", "shop", "shopping", "grocery", "groceries", "order", "amazon", "flipkart") to "🛒",
        listOf("doctor", "dentist", "hospital", "clinic", "checkup") to "🩺",
        listOf("medicine", "meds", "pill", "tablet", "vitamin") to "💊",
        listOf("water", "drink", "hydrate") to "💧",
        listOf("sleep", "bed", "nap") to "😴",
        listOf("wake", "alarm") to "⏰",
        listOf("coffee", "chai", "tea") to "☕",
        listOf("breakfast", "lunch", "dinner", "eat", "food", "cook", "meal") to "🍽️",
        listOf("birthday", "bday") to "🎂",
        listOf("party", "fest", "celebrate") to "🎉",
        listOf("gift", "present") to "🎁",
        listOf("movie", "film", "netflix", "series") to "🎬",
        listOf("music", "guitar", "song", "piano", "sing") to "🎵",
        listOf("game", "gaming") to "🎮",
        listOf("flight", "airport") to "✈️",
        listOf("train") to "🚆",
        listOf("bus", "cab", "uber", "ola") to "🚕",
        listOf("trip", "travel", "pack", "packing", "ticket", "book") to "🧳",
        listOf("clean", "laundry", "wash", "iron") to "🧺",
        listOf("plant", "plants", "garden") to "🪴",
        listOf("pray", "puja", "temple", "mandir") to "🙏",
        listOf("mail", "email", "reply", "message", "text") to "✉️",
        listOf("meet", "meeting", "interview", "standup") to "🤝",
        listOf("haircut", "salon", "barber") to "💇",
        listOf("car", "bike", "service", "repair", "fix") to "🔧",
        listOf("charge", "charger", "battery") to "🔋",
        listOf("journal", "write", "diary", "blog") to "✍️",
        listOf("mom", "maa", "mummy", "dad", "papa", "home", "family", "didi", "bhaiya") to "🏠",
        listOf("friend", "friends", "hangout") to "👯",
        listOf("deadline", "due") to "⏳",
        listOf("idea", "ideas", "brainstorm") to "💡",
        listOf("list", "todo") to "📋",
    )

    fun guess(text: String): String {
        val words = text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        for ((keys, emoji) in rules) {
            for (w in words) {
                if (keys.any { k -> w == k || (k.length >= 4 && w.startsWith(k)) }) return emoji
            }
        }
        return "📌"
    }
}
