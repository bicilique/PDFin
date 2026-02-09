# 📄 PDFin - Professional PDF Toolkit

<div align="center">

![Java](https://img.shields.io/badge/Java-21_LTS-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx)
![Maven](https://img.shields.io/badge/Maven-3.8+-red?style=for-the-badge&logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Tests](https://img.shields.io/badge/Tests-328_Passing-brightgreen?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-macOS_|_Windows_|_Linux-lightgrey?style=for-the-badge)

**A modern, professional desktop application for PDF manipulation with an elegant UI, multi-language support, and powerful features.**

[🚀 Quick Start](#-quick-start) • [✨ Features](#-features) • [📖 Usage Guide](#-usage-guide) • [🏗️ Installation](#-installation) • [🤝 Contributing](#-contributing)

</div>

---

## 📸 Application Preview

### Dashboard
<div align="center">
  <img src="screenshot/PDFIN-Dashboard.jpg" alt="PDFin Dashboard" width="100%" style="max-width: 900px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);" />
  <p><em>Clean and intuitive home screen with quick access to all PDF operations</em></p>
</div>

---

## ✨ Features at a Glance

### 🛠️ Powerful PDF Operations

| Feature | Description | Key Benefit |
|---------|-------------|-------------|
| **Merge** | Combine multiple PDFs into one document | Save time organizing scattered files |
| **Split** | Extract pages or divide documents into parts | Extract what you need quickly |
| **Compress** | Reduce file size up to 90% | Easy sharing and storage |
| **Protect** | Add password security to PDFs | Keep your documents safe |

### 🎨 Premium User Experience

- ✅ **Dark & Light Themes** - Comfortable viewing in any environment
- ✅ **Drag & Drop Interface** - Intuitive file handling
- ✅ **Visual Thumbnails** - See your files before processing
- ✅ **Real-time Progress** - Know what's happening at all times
- ✅ **Smooth Animations** - Professional, polished interactions
- ✅ **Responsive Design** - Works on any screen size

### 🌍 Global Reach

- 🇬🇧 **English** - Complete translation
- 🇮🇩 **Bahasa Indonesia** - Full Indonesian support
- 🔄 **Instant Switching** - Change language without restarting
- 🎯 **Cultural Formatting** - Proper date/time/number formats per locale

---

## 🎯 Key Highlights

### � Project Statistics
- **328 Unit Tests** with 100% pass rate
- **~12,000 Lines** of professional Java code
- **46 Resource Files** including FXML, CSS, and i18n files
- **2 Language** translations with full locale support
- **100% Cross-Platform** compatible (macOS, Windows, Linux)

### 🔒 Security First
- **AES-256 Encryption** for user passwords
- **Input Validation** on all operations
- **Secure File Handling** with proper cleanup
- **Permission Control** with owner/user passwords

### ⚡ Performance
- **Thumbnail Caching** for fast loading
- **Async Operations** for responsive UI
- **Memory Efficient** resource management
- **Batch Processing** for multiple files

---

## 🚀 Quick Start

### The Easiest Way: Download & Run

```bash
# 1. Download from Releases page
# 2. Run directly (Java included)
java -jar pdf-toolkit-1.0.0.jar
```

### From Source (5 minutes)

```bash
# Clone the repository
git clone https://github.com/bicilique/PDFin.git
cd PDFin

# Build and run
mvn clean package
java -jar target/pdf-toolkit-1.0.0.jar
```

---

## 📖 Usage Guide

### 1️⃣ Merge PDFs - Combine Multiple Documents

<div align="center">
  <img src="screenshot/PDFIN-Merge.jpg" alt="Merge Feature" width="100%" style="max-width: 900px; border-radius: 8px; margin: 20px 0;" />
  <p><em>Merge multiple PDFs with drag & drop support and visual reordering</em></p>
</div>

**How to use:**
1. Click **Merge** on the home screen
2. Add files (drag & drop or click Add Files)
3. Reorder by dragging file cards
4. Set output location and filename
5. Click **Merge** - Done!

**Perfect for:**
- Combining reports and documents
- Merging scanned pages into single file
- Creating comprehensive documents from parts

---

### 2️⃣ Split PDFs - Extract Pages You Need

<div align="center">
  <img src="screenshot/PDFIN-Split.jpg" alt="Split Feature" width="100%" style="max-width: 900px; border-radius: 8px; margin: 20px 0;" />
  <p><em>Visual page selection with zoom controls and multiple split modes</em></p>
</div>

**How to use:**
1. Click **Split** on the home screen
2. Select or drag & drop a PDF file
3. Browse pages with visual thumbnails
4. Enter page ranges (e.g., 1-3, 5, 7-9)
5. Choose output folder
6. Click **Split** - Instant extraction!

**Split modes available:**
- By page ranges
- Extract specific pages
- Split into individual pages
- Split by page intervals

**Perfect for:**
- Extracting specific chapters
- Removing unwanted pages
- Distributing parts of documents

---

### 3️⃣ Compress PDFs - Reduce File Size

<div align="center">
  <img src="screenshot/PDFIN-Compress.jpg" alt="Compress Feature" width="100%" style="max-width: 900px; border-radius: 8px; margin: 20px 0;" />
  <p><em>Smart compression with three quality levels and before/after comparison</em></p>
</div>

**How to use:**
1. Click **Compress** on the home screen
2. Add PDF files (batch supported!)
3. Choose compression level:
   - 🟢 **Low** (300 DPI) - Maximum quality, less compression
   - 🟡 **Medium** (150 DPI) - Balanced quality and size
   - 🔴 **High** (72 DPI) - Maximum compression
4. Click **Compress Now**
5. See size reduction instantly

**Perfect for:**
- Email attachment reduction
- Cloud storage optimization
- Faster downloads and sharing

---

### 4️⃣ Protect PDFs - Secure Your Documents

<div align="center">
  <img src="screenshot/PDFIN-Lock.jpg" alt="Protect Feature" width="100%" style="max-width: 900px; border-radius: 8px; margin: 20px 0;" />
  <p><em>Password protection with strength indicator and permission control</em></p>
</div>

**How to use:**
1. Click **Protect** on the home screen
2. Add PDF files to protect
3. Set passwords:
   - **User Password** - Required to open document
   - **Owner Password** - Required to modify permissions
4. Watch real-time password strength indicator
5. Click **Protect PDFs**
6. Files are now encrypted with AES-256!

**Perfect for:**
- Securing confidential documents
- Controlling document permissions
- Preventing unauthorized printing

---

## 📋 System Requirements

### Minimum Specs
- **Java**: Java 21 or higher (included in releases)
- **OS**: macOS 10.14+ | Windows 10+ (64-bit) | Linux (Ubuntu 20.04+)
- **RAM**: 512 MB minimum, 1 GB recommended
- **Disk**: 200 MB available space
- **Display**: 1024x768 minimum resolution

### Recommended Specs
- **RAM**: 2 GB or more
- **Java**: Latest Java 21 LTS
- **Display**: 1920x1080 or higher

---

## 🏗️ Installation

### Option 1: Download Pre-built JAR (Recommended) ⭐

1. **Visit** [Releases Page](https://github.com/bicilique/PDFin/releases)
2. **Download** `pdf-toolkit-1.0.0.jar`
3. **Run**:
   ```bash
   java -jar pdf-toolkit-1.0.0.jar
   ```
4. **That's it!** Application starts immediately

### Option 2: Build from Source

**Prerequisites:**
- Java JDK 21+
- Maven 3.8+
- Git

**Steps:**

```bash
# Clone repository
git clone https://github.com/bicilique/PDFin.git
cd PDFin

# Build with Maven
mvn clean package

# Run application
java -jar target/pdf-toolkit-1.0.0.jar
```

### Option 3: Run Directly with Maven

```bash
git clone https://github.com/bicilique/PDFin.git
cd PDFin
mvn javafx:run
```

---

## ⚙️ Build & Development

```bash
# Compile only
mvn clean compile

# Run all tests
mvn test

# Build without running tests
mvn clean package -DskipTests

# Run in development mode
mvn javafx:run

# View code coverage
mvn clean test jacoco:report
```

---

## 🧰 Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 21 LTS | Core runtime |
| **UI Framework** | JavaFX | 21 | Modern desktop UI |
| **PDF Engine** | Apache PDFBox | 3.0.1 | PDF operations |
| **Build System** | Maven | 3.8+ | Project management |
| **Testing** | JUnit 5 + TestFX | 5.10.1 | Unit & UI tests |
| **i18n** | ResourceBundle | Built-in | Internationalization |

---

## 📁 Project Structure

```
PDFin/
├── src/main/java/com/pdftoolkit/
│   ├── controllers/          # UI Controllers (MVC)
│   ├── services/             # PDF Operations Layer
│   ├── ui/                   # Custom UI Components
│   ├── utils/                # Utilities (i18n, themes)
│   ├── navigation/           # State Management
│   └── Main.java             # Application Entry Point
│
├── src/main/resources/
│   ├── css/                  # Stylesheets
│   ├── icons/                # Icon Assets
│   ├── views/                # FXML Layouts
│   ├── messages_en.properties    # English i18n
│   └── messages_id.properties    # Indonesian i18n
│
├── src/test/java/            # 18 Test Classes (328 tests)
├── pom.xml                   # Maven Configuration
└── README.md                 # This file
```

---

## 🎨 Customization Guide

### Adding a New Language

1. **Create** translation file in `src/main/resources/`:
   ```
   messages_es.properties  (for Spanish, etc.)
   ```

2. **Add** all translation keys matching `messages_en.properties`

3. **Update** `LocaleManager.java`:
   ```java
   supportedLocales.put("es", new Locale("es"));
   ```

4. **Restart** application - new language appears immediately

### Modifying the Theme

Edit `src/main/resources/css/app.css`:

```css
/* Light Mode */
.root[data-theme="light"] {
    -fx-base: #ffffff;
    -fx-control-inner-background: #f5f5f5;
}

/* Dark Mode */
.root[data-theme="dark"] {
    -fx-base: #1e1e1e;
    -fx-control-inner-background: #2d2d2d;
}
```

---

## 🤝 Contributing

We'd love your contributions! Here's how:

### 1. Fork & Clone
```bash
git clone https://github.com/YOUR-USERNAME/PDFin.git
cd PDFin
```

### 2. Create Feature Branch
```bash
git checkout -b feature/amazing-feature
```

### 3. Make Changes & Test
```bash
# Run tests to ensure nothing breaks
mvn test
```

### 4. Commit & Push
```bash
git commit -m "Add amazing feature"
git push origin feature/amazing-feature
```

### 5. Open Pull Request
Click "New Pull Request" on GitHub

### Code Standards
- ✅ Follow Java naming conventions
- ✅ Add JavaDoc for public methods
- ✅ Keep methods under 50 lines
- ✅ Write descriptive commit messages
- ✅ Pass all tests before submitting PR

---

## 🐛 Issues & Support

### Found a Bug?
1. **Check** [GitHub Issues](https://github.com/bicilique/PDFin/issues) for duplicates
2. **Open** new issue with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots (if applicable)
   - OS and Java version

### Need Help?
- 💬 [GitHub Discussions](https://github.com/bicilique/PDFin/discussions)
- 📧 Check existing documentation
- 🔍 Browse closed issues for solutions

---

## 📊 Features Comparison

### PDFin vs Alternatives

| Feature | PDFin | Free Alternatives |
|---------|-------|-------------------|
| **Price** | Free & Open Source | Free or Paid |
| **No Ads/Tracking** | ✅ Yes | ❌ No |
| **Offline Use** | ✅ Yes | ❌ Often requires cloud |
| **Dark Mode** | ✅ Yes | ❌ Limited |
| **Multiple Languages** | ✅ Yes | ❌ Limited |
| **Merge PDFs** | ✅ Yes | ✅ Yes |
| **Split PDFs** | ✅ Yes | ✅ Yes |
| **Compress PDFs** | ✅ Yes | ❌ No |
| **Protect PDFs** | ✅ Yes | ❌ No |
| **Cross-Platform** | ✅ macOS, Windows, Linux | ⚠️ Limited |

---

## 🙏 Acknowledgments

### Open Source Libraries
- [Apache PDFBox](https://pdfbox.apache.org/) - PDF manipulation engine
- [JavaFX](https://openjfx.io/) - Modern UI framework
- [Tabler Icons](https://tabler-icons.io/) - Beautiful icon set

### Contributors
- Development team for core features
- QA team for comprehensive testing
- Design team for beautiful UI
- Community for feedback and suggestions

---

## � License

This project is licensed under the **MIT License** - see [LICENSE](LICENSE) file for details.

**MIT License means:**
- ✅ Free for commercial use
- ✅ Free for private use
- ✅ Modify freely
- ✅ Distribute freely
- ⚠️ Include license and copyright notice

---

## 🗺️ Roadmap

### Version 1.1.0 (Planned)
- PDF rotation feature
- Page reordering
- Watermark tool
- PDF to image conversion
- More language support

### Version 1.2.0 (Future)
- PDF form filling
- Digital signatures
- OCR text extraction
- PDF comparison
- Cloud integration

### Version 2.0.0 (Long Term)
- Plugin system
- Command-line interface
- REST API
- Batch automation
- Advanced features

---

<div align="center">

## ⭐ Show Your Support

If PDFin helps you, please give it a star! It helps other people discover this project.

### Star ⭐ | Fork 🍴 | Share 🚀

---

**Made with ❤️ using Java & JavaFX**

[GitHub](https://github.com/bicilique/PDFin) • [Issues](https://github.com/bicilique/PDFin/issues) • [Discussions](https://github.com/bicilique/PDFin/discussions)

© 2026 PDFin - All rights reserved

</div>
