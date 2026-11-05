plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.5"

stonecutter parameters {
    constants["release"] = false

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
