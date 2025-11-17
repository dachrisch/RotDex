# 🧠 RotDex - Collect the Chaos

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Android](https://img.shields.io/badge/Android-24%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple)
![License](https://img.shields.io/badge/license-MIT-orange)

## 🎴 What is RotDex?

**RotDex** is a revolutionary Android app that transforms your wildest thoughts, memes, and internet culture into collectible AI-generated cards. Think Pokédex meets the chaotic energy of modern internet culture. Each card is a unique, AI-powered creation that captures the essence of brainrot in beautiful, shareable form.

### ✨ Why RotDex?

In a world drowning in content, RotDex turns fleeting moments of inspiration into lasting digital collectibles. Whether it's a random shower thought, a viral meme concept, or pure creative chaos - RotDex immortalizes it as a stunning card you can treasure forever.

## 🚀 Features

- **🎨 AI-Powered Generation**: Transform any text prompt into unique, artistic cards using cutting-edge AI
- **📚 Personal Collection**: Build your own library of brainrot masterpieces
- **🌟 Rarity System**: Cards come in Common, Rare, Epic, and Legendary rarities
- **💾 Local Storage**: Your collection is yours - stored securely on your device
- **🎭 Beautiful UI**: Modern Material Design 3 with dark theme support
- **⚡ Fast & Smooth**: Built with Jetpack Compose for buttery-smooth performance
- **🔄 Share & Export**: Show off your best cards to friends

## 🎯 Vision

RotDex isn't just an app - it's a creative playground where:
- **Artists** can experiment with AI-generated concepts
- **Meme lords** can immortalize their best ideas
- **Collectors** can build unique digital galleries
- **Everyone** can turn chaos into creativity

## 🏗️ Tech Stack

RotDex is built with modern Android development best practices:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (for local card storage)
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Navigation Compose
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📁 Project Structure

```
RotDex/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/rotdex/
│   │   │   │   ├── data/           # Data layer (models, database, API)
│   │   │   │   ├── ui/             # UI layer (screens, components, theme)
│   │   │   │   ├── viewmodel/      # ViewModels
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                # Resources (layouts, strings, colors)
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/            # Instrumented tests
│   │   └── test/                   # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── docs/                           # Documentation
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🎮 Planned Features

- [ ] Multiple AI model support
- [ ] Card trading between users
- [ ] Achievement system
- [ ] Daily challenges
- [ ] Card customization (borders, effects)
- [ ] Gallery view with filters
- [ ] Export to social media
- [ ] Cloud sync (optional)
- [ ] Card battle mini-game

## 🛠️ Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK 34
- Gradle 8.2+

### Building the App

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Configure your AI API keys (instructions in `docs/`)
5. Build and run on your device or emulator

```bash
./gradlew assembleDebug
```

## 🎨 Design Philosophy

RotDex embraces:
- **Chaos as Creativity**: Every wild idea deserves a beautiful form
- **Ownership**: Your cards, your collection, your data
- **Joy**: Making creativity fun and accessible
- **Quality**: Stunning visuals powered by AI
- **Community**: Sharing and celebrating creativity together

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

We welcome contributions! Whether it's:
- 🐛 Bug reports
- 💡 Feature suggestions
- 🎨 UI/UX improvements
- 📝 Documentation
- 💻 Code contributions

Check out our contributing guidelines in `docs/CONTRIBUTING.md`

## 💬 Contact & Community

- **Issues**: [GitHub Issues](https://github.com/yourusername/rotdex/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/rotdex/discussions)

## 🙏 Acknowledgments

Built with inspiration from:
- The vibrant internet culture community
- Amazing AI art generators
- The Android development community
- Everyone who's ever had a weird thought worth sharing

---

**Made with 🧠 and ❤️ for the chaos collectors**

*"In a world of fleeting thoughts, RotDex makes them eternal"*
