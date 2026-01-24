# 📄 Desktop PDF Helper

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

A modern, elegant desktop application for PDF manipulation with multi-language support and beautiful UI.

[Features](#-features) • [Installation](#-installation) • [Usage](#-usage) • [Building](#-building) • [Contributing](#-contributing)

</div>

---

## ✨ Features

### 🛠️ PDF Operations
- **Merge PDFs** - Combine multiple PDF files into a single document
- **Split PDFs** - Extract specific pages or split into multiple files
- **Compress PDFs** - Reduce file size with adjustable quality levels
- **Protect PDFs** - Add password protection to secure your documents

### 🎨 Modern UI
- **Dark/Light Mode** - Comfortable viewing in any environment
- **Smooth Animations** - Polished transitions and hover effects
- **Responsive Design** - Clean, intuitive interface
- **Modern Icons** - Beautiful Tabler icons throughout

### 🌍 Multi-Language Support
- **English** - Full English translation
- **Bahasa Indonesia** - Complete Indonesian translation
- **Real-time Switching** - Change language on-the-fly without restart

### 💡 User Experience
- **Drag & Drop** - Easy file selection
- **Progress Tracking** - Visual feedback for operations
- **Error Handling** - Clear, helpful error messages
- **Keyboard Shortcuts** - Efficient workflow

---

## 📋 Requirements

- **Java** 21 or higher
- **Maven** 3.8 or higher
- **Operating System**: macOS, Windows, or Linux

---

## 🚀 Installation

### Option 1: Download Pre-built Release
1. Go to [Releases](https://github.com/yourusername/desktop-pdf-helper/releases)
2. Download the latest `.jar` file
3. Run with: `java -jar desktop-pdf-helper-1.0.0.jar`

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/desktop-pdf-helper.git
cd desktop-pdf-helper

# Build with Maven
mvn clean package

# Run the application
mvn javafx:run
```

---

## 📖 Usage

### Starting the Application

```bash
# Using Maven
mvn javafx:run

# Or using the run script (macOS/Linux)
./run.sh

# Or directly with Java (after building)
java -jar target/desktop-pdf-helper-1.0.0.jar
```

### Quick Guide

#### Merging PDFs
1. Click **Merge** from the home screen
2. Click **Add Files** or drag & drop PDF files
3. Reorder files if needed
4. Click **Merge** and choose output location

#### Splitting PDFs
1. Click **Split** from the home screen
2. Select a PDF file
3. Choose split method (by pages, by range, etc.)
4. Click **Split** and choose output folder

#### Compressing PDFs
1. Click **Compress** from the home screen
2. Select PDF file(s)
3. Choose compression level (Low, Medium, High)
4. Click **Compress** and save

#### Protecting PDFs
1. Click **Protect** from the home screen
2. Select PDF file
3. Enter password (view strength indicator)
4. Click **Protect** and save

### Language Switching
- Click the language button in the navigation pane
- Select **English** or **Bahasa Indonesia**
- UI updates instantly

### Theme Switching
- Click the theme icon in the navigation pane
- Toggle between **Dark Mode** and **Light Mode**
- Preference is saved automatically

---

## 🏗️ Building

### Compile Only
```bash
mvn clean compile
```

### Package (Create JAR)
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Skip Tests During Build
```bash
mvn clean package -DskipTests
```

---

## 🧰 Technology Stack

| Technology | Purpose |
|-----------|---------|
| **Java 21** | Core programming language |
| **JavaFX 21** | UI framework |
| **Apache PDFBox** | PDF manipulation library |
| **Maven** | Dependency management & build tool |
| **ResourceBundle** | Internationalization (i18n) |

---

## 📁 Project Structure

```
desktop-pdf-helper/
├── src/
│   ├── main/
│   │   ├── java/com/pdftoolkit/
│   │   │   ├── controllers/       # UI controllers
│   │   │   ├── services/          # PDF operations
│   │   │   ├── ui/                # Custom UI components
│   │   │   ├── utils/             # Utilities (i18n, theme)
│   │   │   └── Main.java          # Entry point
│   │   └── resources/
│   │       ├── css/               # Stylesheets
│   │       ├── icons/             # UI icons
│   │       ├── views/             # FXML layouts
│   │       ├── messages_en.properties   # English translations
│   │       └── messages_id.properties   # Indonesian translations
│   └── test/                      # Unit tests
├── pom.xml                        # Maven configuration
└── README.md                      # This file
```

---

## 🎨 Customization

### Adding New Languages
1. Create `messages_XX.properties` in `src/main/resources/`
2. Add all translation keys
3. Update `LocaleManager.java` to include the new locale

### Modifying Themes
Edit `src/main/resources/css/app.css`:
- Light mode: `.root[data-theme="light"]` section
- Dark mode: `.root[data-theme="dark"]` section

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Style
- Follow Java naming conventions
- Add JavaDoc comments for public methods
- Keep methods focused and under 50 lines
- Write meaningful commit messages

---

## 🐛 Bug Reports

Found a bug? Please open an issue with:
- **Description** of the problem
- **Steps to reproduce**
- **Expected behavior**
- **Screenshots** (if applicable)
- **System information** (OS, Java version)

---

## 🙏 Acknowledgments

- [Apache PDFBox](https://pdfbox.apache.org/) - PDF manipulation library
- [Tabler Icons](https://tabler-icons.io/) - Beautiful icon set
- [JavaFX](https://openjfx.io/) - Rich UI framework

---

## 📞 Contact

- **Issues**: [GitHub Issues](https://github.com/bicilique/PDFin/issues)
- **Discussions**: [GitHub Discussions](https://github.com/bicilique/PDFin)

---

<div align="center">

**Made with ❤️ using Java & JavaFX**

⭐ Star this repo if you find it helpful!

</div>
