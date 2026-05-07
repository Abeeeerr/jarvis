<div align="center">

<br/>

```
     ██╗ █████╗ ██████╗ ██╗   ██╗██╗███████╗
     ██║██╔══██╗██╔══██╗██║   ██║██║██╔════╝
     ██║███████║██████╔╝██║   ██║██║███████╗
██   ██║██╔══██║██╔══██╗╚██╗ ██╔╝██║╚════██║
╚█████╔╝██║  ██║██║  ██║ ╚████╔╝ ██║███████║
 ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚═╝╚══════╝
```

### **J.A.R.V.I.S. // OS**
*Just A Rather Very Intelligent System*

<br/>

![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-00f2ff?style=for-the-badge&labelColor=0a0e14)
![Java](https://img.shields.io/badge/Java-21-00f2ff?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0a0e14)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-00f2ff?style=for-the-badge&logo=springboot&logoColor=white&labelColor=0a0e14)
![License](https://img.shields.io/badge/license-MIT-00f2ff?style=for-the-badge&labelColor=0a0e14)
![Status](https://img.shields.io/badge/status-ONLINE-00ff88?style=for-the-badge&labelColor=0a0e14)
[![Live Demo](https://img.shields.io/badge/Live_Demo-abeeeerr.github.io%2Fjarvis-00f2ff?style=for-the-badge&labelColor=0a0e14)](https://abeeeerr.github.io/jarvis)

<br/>

> **This is V1.** A fully local, zero-cost, voice-enabled AI assistant with a cinematic JARVIS OS dashboard.
> Many updates, new skills, and features are on the way. Star the repo to stay updated.

<br/>

</div>

---

## ✨ What is JARVIS?

JARVIS is a **personal AI voice assistant** built entirely in Java — inspired by Tony Stark's iconic system. It runs **100% locally on your machine** with no cloud dependencies, no subscriptions, and no API keys. Just you, your hardware, and your own AI.

It features a **cinematic JARVIS OS web dashboard** with a real-time voice interface, system telemetry, analytics, communications panel, and environment management — all wired to a powerful skill-based backend that can understand natural language, answer questions, open apps, search the web, take notes, set reminders, play music, and more.

---

## 🚀 Features

### 🎙️ Voice Interface
- **Offline Speech-to-Text** via Vosk — no internet required for listening
- **Natural Language Understanding** powered by Gemma 3 4B running locally via Ollama
- **Text-to-Speech** via macOS built-in `say` command (Daniel voice)
- Click the waveform or say the command — Jarvis does the rest

### 🖥️ JARVIS OS Dashboard
- **Core Tab** — System status, CPU/RAM telemetry, live weather, real-time data stream log, listening animation
- **Analytics Tab** — Live market flux chart, neural load gauge, global map feed, intelligence hub stats
- **Environment Tab** — Floor plan visualization, climate tuning controls, camera feed simulation, power & air quality charts
- **Comms Tab** — Priority alerts, incoming transmission panel, active link monitoring
- **Terminal Overlay** — Command-line interface built into the dashboard

### 🧠 Skills (15 and growing)

| Category | Skills |
|---|---|
| **Time & Date** | Current time, today's date |
| **Information** | Wikipedia lookup, web search (opens Chrome), weather |
| **AI Chat** | General conversation, coding help, homework assistance |
| **Productivity** | Take notes, read notes, list notes, set reminders, list reminders |
| **System** | Open any macOS app by name (Chrome, Spotify, VS Code, etc.) |
| **Entertainment** | Play / stop music, tell jokes |

### 💡 Zero Cost Architecture
| Component | Technology | Cost |
|---|---|---|
| Speech-to-Text | Vosk (offline) | Free |
| AI / NLP Brain | Ollama + Gemma 3 4B (local LLM) | Free |
| Text-to-Speech | macOS `say` command | Free |
| Web Search | DuckDuckGo API | Free |
| Weather | Open-Meteo API | Free |
| Wikipedia | Wikipedia REST API | Free |

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 3.2.5, Spring WebSocket (STOMP)
- **AI / NLP:** Ollama + Gemma 3 4B (local), Vosk STT (offline)
- **Frontend:** Vanilla JS, Chart.js, Space Grotesk + Inter fonts
- **Build:** Maven
- **Platform:** macOS (designed for Apple Silicon / Intel Mac)

---

## ⚙️ Setup & Installation

### Prerequisites

| Tool | Install |
|---|---|
| Java 21+ | `brew install openjdk@21` |
| Maven | `brew install maven` |
| Ollama | [ollama.com](https://ollama.com) → Download |

### 1. Clone the repo

```bash
git clone https://github.com/Abeeeerr/jarvis.git
cd jarvis
```

### 2. Download the Vosk speech model

```bash
# Download a lightweight English model (~40MB)
curl -LO https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
mv vosk-model-small-en-us-0.15/* src/main/resources/model/
rm -rf vosk-model-small-en-us-0.15 vosk-model-small-en-us-0.15.zip
```

### 3. Start Ollama and pull Gemma 3

```bash
# Install Ollama then run:
ollama serve &
ollama pull gemma3:4b
```

> The first `ollama pull` downloads ~3.3GB. This only happens once.

### 4. Run JARVIS

```bash
mvn spring-boot:run
```

Wait for:
```
Started JarvisApplication in X.XXX seconds
```

### 5. Open the dashboard

Open **Chrome or Edge** (required for microphone support):
```
http://localhost:8080
```

---

## 🗣️ How to Use

| Action | How |
|---|---|
| Voice command | Click the **waveform circle** or the **VOICE_OVERRIDE** button |
| Type a command | Use the input bar at the bottom of the Core tab |
| Terminal mode | Click **TERMINAL ▶** in the top-right corner |
| Switch views | Use the top nav bar or left sidebar |

### Example Commands

```
"What time is it?"
"Search for the latest iPhone"
"Open Chrome"
"Open Spotify"
"What's the weather in London?"
"Take a note: pick up groceries"
"Remind me to call mom at 6pm"
"Write a Python function to sort a list"
"Tell me a joke"
"What is quantum computing?"
```

---

## 📁 Project Structure

```
jarvis/
├── src/main/java/com/jarvis/
│   ├── api/                    # REST + WebSocket controllers
│   ├── core/                   # Skill dispatcher
│   ├── nlp/                    # GemmaService (Ollama), IntentEngine, EntityExtractor
│   ├── stt/                    # VoskSTTService (offline speech-to-text)
│   ├── tts/                    # TTSService (macOS say)
│   └── skills/
│       ├── chat/               # GeneralChat, Coding, Homework
│       ├── greet/              # Greet
│       ├── info/               # Wikipedia
│       ├── music/              # Music play/stop
│       ├── notes/              # Notes CRUD
│       ├── reminder/           # Reminders
│       ├── time/               # Time & Date
│       ├── weather/            # Weather (Open-Meteo)
│       └── web/                # WebSearch, AppLaunch
├── src/main/resources/
│   ├── static/                 # Frontend (index.html, style.css, app.js)
│   ├── model/                  # Vosk model (not tracked in git)
│   └── application.properties
└── pom.xml
```

---

## 🗺️ Roadmap

This is **Version 1.0** — the foundation. Here's what's coming:

- [ ] **Smart Home Integration** — control lights, thermostat, plugs via JARVIS
- [ ] **Face Recognition** — identify who's speaking and personalize responses
- [ ] **Calendar & Email** — read emails, manage Google Calendar by voice
- [ ] **Multi-room Audio** — broadcast JARVIS across speakers in your home
- [ ] **Memory System** — JARVIS remembers past conversations and learns preferences
- [ ] **Mobile App** — companion app to control JARVIS from your phone
- [ ] **Plugin SDK** — build and share your own skills as plugins
- [ ] **Wake Word** — "Hey JARVIS" — always-on hotword detection
- [ ] **Windows / Linux support** — cross-platform TTS and system commands
- [ ] **Vision** — describe what's on your screen using a multimodal model
- [ ] **Voice Cloning** — custom JARVIS voice instead of macOS Daniel

---

## 🤝 Contributing

Contributions, ideas, and skill suggestions are welcome! This project is in active early development.

1. Fork the repo
2. Create a branch: `git checkout -b feature/my-new-skill`
3. Add your skill by implementing the `Skill` interface and annotating with `@Component`
4. Open a Pull Request

**Adding a new skill is simple:**
```java
@Component
public class MySkill implements Skill {
    @Override public String getIntentName() { return "MY_INTENT"; }
    @Override public String execute(Map<String, String> entities) {
        return "Done!";
    }
}
```
Spring and the `SkillRegistry` auto-register it — no wiring needed.

---

## ⚠️ Platform Note

JARVIS V1 is built and tested on **macOS**. The TTS (`say` command) and app launcher (`open -a`) are macOS-specific. Windows/Linux support is on the roadmap.

---

## 📄 License

MIT License — free to use, modify, and distribute. See [LICENSE](LICENSE) for details.

---

<div align="center">

**Built with Java, curiosity, and a love for Iron Man.**

*V1 is just the beginning. The best is yet to come.*

<br/>

[![GitHub](https://img.shields.io/badge/GitHub-Abeeeerr-00f2ff?style=for-the-badge&logo=github&logoColor=white&labelColor=0a0e14)](https://github.com/Abeeeerr)

</div>
