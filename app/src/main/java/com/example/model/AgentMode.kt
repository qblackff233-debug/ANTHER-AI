package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.ModeAnalystColor
import com.example.ui.theme.ModeCoderColor
import com.example.ui.theme.ModeGeneralColor
import com.example.ui.theme.ModeStorytellerColor
import com.example.ui.theme.ModeTranslatorColor
import com.example.ui.theme.ModeTutorColor
import com.example.ui.theme.ModeWriterColor

data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val promptText: String
)

enum class AgentMode(
    val displayName: String,
    val shortDescription: String,
    val badgeLabel: String,
    val accentColor: Color,
    val icon: ImageVector,
    val capabilities: List<String>,
    val thinkingPhases: List<String>,
    val starterPrompts: List<StarterPrompt>
) {
    GENERAL_ASSISTANT(
        displayName = "General Assistant",
        shortDescription = "Smart everyday AI for Q&A, brainstorming & bilingual chat",
        badgeLabel = "All-Rounder",
        accentColor = ModeGeneralColor,
        icon = Icons.Rounded.AutoAwesome,
        capabilities = listOf(
            "Answers complex questions clearly",
            "Brainstorms plans & daily workflows",
            "Fluent in English, Hinglish & Hindi"
        ),
        thinkingPhases = listOf(
            "Understanding your request...",
            "Connecting relevant context...",
            "Structuring a clear answer..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Plan a focused week",
                "Productivity routine & deep work blocks",
                "Help me design a realistic 5-day productivity routine balancing deep work, fitness, and rest."
            ),
            StarterPrompt(
                "Explain in Hinglish",
                "Compound interest samjhao simple words mein",
                "Compound interest aur SIP kaise kaam karte hain? Simple Hinglish mein real-life example ke saath samjhao."
            ),
            StarterPrompt(
                "Brainstorm side projects",
                "Weekend app ideas with real utility",
                "Give me 5 unique weekend app ideas that solve genuine everyday frustrations and explain the core hook for each."
            ),
            StarterPrompt(
                "Compare & decide",
                "Trade-offs table for quick choices",
                "Create a crisp pros-and-cons comparison table between remote work, hybrid work, and office-first teams."
            )
        )
    ),
    CODER(
        displayName = "Coder",
        shortDescription = "Clean architecture, debugging, algorithms & code reviews",
        badgeLabel = "Engineering",
        accentColor = ModeCoderColor,
        icon = Icons.Rounded.Code,
        capabilities = listOf(
            "Idiomatic code with clean structure",
            "Root-cause bug & edge-case analysis",
            "Time & space complexity breakdowns"
        ),
        thinkingPhases = listOf(
            "Parsing technical requirements...",
            "Evaluating edge cases & architecture...",
            "Drafting clean, idiomatic code..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Kotlin Flow debounce",
                "Search bar state management pattern",
                "Write a clean Kotlin StateFlow search pattern using debounce, distinctUntilChanged, and flatMapLatest with error handling."
            ),
            StarterPrompt(
                "Debug race condition",
                "Fix async state bugs in coroutines",
                "Explain common coroutine race conditions in Android ViewModels and show a thread-safe fix using Mutex or StateFlow.update."
            ),
            StarterPrompt(
                "SQL query tuning",
                "Optimize slow composite indexes",
                "How do composite B-tree indexes work in SQL? Show a concrete before/after query optimization example."
            ),
            StarterPrompt(
                "Design rate limiter",
                "Token bucket algorithm in Python",
                "Implement a clean Token Bucket rate limiter class in Python and explain its time/space complexity."
            )
        )
    ),
    CONTENT_WRITER(
        displayName = "Content Writer",
        shortDescription = "Polished articles, emails, hooks & tone-matched copy",
        badgeLabel = "Editorial",
        accentColor = ModeWriterColor,
        icon = Icons.Rounded.EditNote,
        capabilities = listOf(
            "Engaging hooks & narrative flow",
            "Emails, blogs & social threads",
            "Adaptive tone from formal to Hinglish"
        ),
        thinkingPhases = listOf(
            "Identifying audience & tone...",
            "Crafting an engaging hook...",
            "Polishing rhythm and clarity..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Launch announcement",
                "Crisp product post with strong hook",
                "Write a punchy product launch post for a personal AI productivity companion—keep it human, modern, and hype-free."
            ),
            StarterPrompt(
                "Polite follow-up email",
                "Professional nudge without awkwardness",
                "Draft a warm yet concise follow-up email checking in on a project proposal sent five days ago."
            ),
            StarterPrompt(
                "Hinglish creator script",
                "60-second tech reel with high retention",
                "Write a natural 60-second Hinglish video script explaining 3 hidden habits of high-output builders."
            ),
            StarterPrompt(
                "Executive summary",
                "Turn messy notes into crisp bullets",
                "Show me a clean executive summary template for turning raw meeting notes into decisions, owners, and deadlines."
            )
        )
    ),
    STUDY_TUTOR(
        displayName = "Study Tutor",
        shortDescription = "Step-by-step concepts, intuitive analogies & quick quizzes",
        badgeLabel = "Learning",
        accentColor = ModeTutorColor,
        icon = Icons.Rounded.School,
        capabilities = listOf(
            "Breaks down hard topics step by step",
            "Uses memorable visual analogies",
            "Checks understanding with mini-questions"
        ),
        thinkingPhases = listOf(
            "Breaking concept into core blocks...",
            "Finding an intuitive analogy...",
            "Preparing a quick check-in..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Transformers intuition",
                "How attention works without heavy math",
                "Explain how self-attention in AI Transformers works using an intuitive everyday analogy, then add a 2-question check."
            ),
            StarterPrompt(
                "Organic chemistry trick",
                "Memorize reaction mechanisms easily",
                "Teach me how nucleophiles and electrophiles behave so I can predict organic reactions instead of rote memorizing."
            ),
            StarterPrompt(
                "OS threads vs processes",
                "Clear table + Hindi/Hinglish recap",
                "Explain Processes vs Threads with a clean comparison table and a quick Hinglish summary at the end."
            ),
            StarterPrompt(
                "Active recall plan",
                "Master a tough exam in 10 days",
                "Design a 10-day spaced repetition and active recall study framework for a dense technical syllabus."
            )
        )
    ),
    CREATIVE_STORYTELLER(
        displayName = "Creative Storyteller",
        shortDescription = "Imaginative fiction, world-building, dialogue & plot twists",
        badgeLabel = "Fiction",
        accentColor = ModeStorytellerColor,
        icon = Icons.Rounded.AutoStories,
        capabilities = listOf(
            "Atmospheric world-building & lore",
            "Distinct character voices & tension",
            "Interactive branching story continuations"
        ),
        thinkingPhases = listOf(
            "Imagining setting & atmosphere...",
            "Shaping character tension...",
            "Weaving sensory narrative details..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Clockwork monsoon",
                "Atmospheric sci-fi short opening",
                "Write the opening scene of a cozy-noir sci-fi story set in a floating monsoon city where memories are traded as brass keys."
            ),
            StarterPrompt(
                "Build a magic system",
                "Rules, costs & cultural impact",
                "Invent an original magic system powered by acoustics and silence—define its rules, physical cost, and how cities adapt to it."
            ),
            StarterPrompt(
                "Dialogue with subtext",
                "Two rivals meeting on a night train",
                "Write a tense 12-line dialogue between two estranged friends meeting unexpectedly on a midnight express train."
            ),
            StarterPrompt(
                "Interactive quest",
                "Choose-your-path mystery intro",
                "Start an interactive mystery set in an abandoned lighthouse and end with 3 intriguing choices for what I do next."
            )
        )
    ),
    DATA_ANALYST(
        displayName = "Data Analyst",
        shortDescription = "Interpret metrics, spot trends & render visual chart cards",
        badgeLabel = "Analytics",
        accentColor = ModeAnalystColor,
        icon = Icons.Rounded.BarChart,
        capabilities = listOf(
            "Synthesizes tables, CSVs & KPIs",
            "Highlights anomalies & growth drivers",
            "Renders in-chat visual bar & line charts"
        ),
        thinkingPhases = listOf(
            "Inspecting metrics & distributions...",
            "Detecting trends & anomalies...",
            "Generating tables & chart visuals..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "Visualize quarterly growth",
                "Sample SaaS revenue chart + insights",
                "Analyze these quarterly SaaS metrics and visualize them: Q1 $42k, Q2 $58k, Q3 $64k, Q4 $91k. Highlight growth drivers and risks."
            ),
            StarterPrompt(
                "Compare funnel drop-off",
                "Signup conversion chart & fixes",
                "Break down a mobile onboarding funnel (Visit 100%, Signup 48%, Profile 29%, First Action 18%, Day-7 Retained 9%) with a chart and top 3 experiments."
            ),
            StarterPrompt(
                "Explain A/B test stats",
                "P-value, power & sample size guide",
                "Create a practical A/B testing cheat-sheet table covering sample size, statistical power, p-value, and common peeking mistakes."
            ),
            StarterPrompt(
                "Analyze pasted CSV",
                "Paste rows or attach a CSV file",
                "I want to analyze a dataset—tell me what metrics you look for first and give a sample insight + chart format."
            )
        )
    ),
    LANGUAGE_TRANSLATOR(
        displayName = "Language Translator",
        shortDescription = "Nuanced multi-language translation with tone & script notes",
        badgeLabel = "Polyglot",
        accentColor = ModeTranslatorColor,
        icon = Icons.Rounded.Translate,
        capabilities = listOf(
            "Preserves idioms & cultural nuance",
            "Formal, casual & Hinglish registers",
            "Includes transliteration & usage tips"
        ),
        thinkingPhases = listOf(
            "Detecting source nuance & idiom...",
            "Selecting natural target phrasing...",
            "Adding tone & pronunciation notes..."
        ),
        starterPrompts = listOf(
            StarterPrompt(
                "English to Hindi & Hinglish",
                "Natural + formal variants side-by-side",
                "Translate 'Let's align on the deliverables before Friday so there are no last-minute surprises' into natural Hinglish, casual Hindi, and formal Hindi."
            ),
            StarterPrompt(
                "Multilingual greeting card",
                "Japanese, Spanish & French nuances",
                "How do you warmly say 'Wishing you courage and joy in your new chapter' in Japanese, Spanish, and French with pronunciation?"
            ),
            StarterPrompt(
                "Translate & localize UI",
                "Compare literal vs natural phrasing",
                "Translate these app phrases into Spanish and German, showing both literal and natural UX copy in a table: 'Save changes', 'Try again', 'Syncing...'."
            ),
            StarterPrompt(
                "Hinglish to crisp English",
                "Polish casual WhatsApp into pro email",
                "Rewrite this Hinglish message into polished executive English: 'Bhai kal ka demo thoda rush ho raha hai, can we push by 2 days so quality solid rahe?'"
            )
        )
    );

    fun buildSystemInstruction(hinglishPreferred: Boolean): String {
        val bilingualRule = if (hinglishPreferred) {
            "The user has enabled Hinglish/Hindi preference: naturally blend clear Hinglish or Hindi (matching the user's script—Roman Hinglish or Devanagari) whenever conversational or helpful, while keeping technical terms crisp."
        } else {
            "Respond in the user's language (English by default, or fluent Hinglish/Hindi whenever the user writes in or requests Hindi/Hinglish)."
        }

        val persona = when (this) {
            GENERAL_ASSISTANT -> """
                You are Aether AI in General Assistant mode: a smart, warm, concise, and versatile personal AI agent.
                Structure answers clearly with Markdown headings, bullet points, or comparison tables when useful.
            """.trimIndent()
            CODER -> """
                You are Aether AI in Coder mode: a senior staff software engineer.
                Provide clean, production-grade code inside fenced Markdown code blocks with explicit language tags (e.g., ```kotlin, ```python, ```sql).
                Explain architectural rationale, edge cases, and time/space complexity concisely.
            """.trimIndent()
            CONTENT_WRITER -> """
                You are Aether AI in Content Writer mode: an editorial strategist and copywriter.
                Craft engaging hooks, vivid phrasing, and well-paced structure tailored to the requested format (blog, email, script, social post).
                Offer a brief note on tone or an alternate punchy headline when helpful.
            """.trimIndent()
            STUDY_TUTOR -> """
                You are Aether AI in Study Tutor mode: a patient, encouraging Socratic mentor.
                Break complex topics into digestible steps, anchor abstract ideas in concrete real-world analogies, highlight common pitfalls, and end with 1-2 short check-your-understanding questions.
            """.trimIndent()
            CREATIVE_STORYTELLER -> """
                You are Aether AI in Creative Storyteller mode: a master fiction writer and world-builder.
                Write evocative scenes with strong sensory imagery, subtext-rich dialogue, and distinct character voice.
                Avoid clichés and give the user intriguing narrative threads or optional story directions to explore next.
            """.trimIndent()
            DATA_ANALYST -> """
                You are Aether AI in Data Analyst mode: a sharp quantitative analyst.
                Interpret numbers, distributions, and trends rigorously using Markdown tables and clear takeaways.
                CRITICAL VISUALIZATION RULE: Whenever the prompt contains numeric data, comparisons, time-series values, or asks for a chart/visualization, include ONE valid JSON chart block using this exact tag format:
                [CHART]{"title":"Short Chart Title","type":"bar","unit":"% or $ or empty","items":[{"label":"Q1","value":42.0},{"label":"Q2","value":58.0}]}[/CHART]
                Use "type":"line" for time-series/trends or "type":"bar" for comparisons. Keep labels short (under 14 chars) and limit items to 2-8 points.
            """.trimIndent()
            LANGUAGE_TRANSLATOR -> """
                You are Aether AI in Language Translator mode: an expert polyglot linguist and cultural localizer.
                Provide accurate, natural translations rather than stiff word-for-word rendering.
                Present Formal vs Casual options in clean Markdown tables when helpful, include Romanized transliteration for non-Latin scripts (like Hindi/Japanese/Arabic), and briefly explain key idiomatic nuances.
            """.trimIndent()
        }

        val followUpFormat = """
            At the very end of your response, include 2 or 3 brief, helpful follow-up prompts the user might tap next, formatted strictly on a single line as:
            [FOLLOWUPS]Follow-up 1 | Follow-up 2 | Follow-up 3[/FOLLOWUPS]
        """.trimIndent()

        return "$persona\n\n$bilingualRule\n\n$followUpFormat"
    }

    companion object {
        fun fromName(raw: String?): AgentMode =
            entries.firstOrNull { it.name == raw } ?: GENERAL_ASSISTANT
    }
}
