# PDFin - Release Notes

## Version 1.0.0 - Initial Release (January 31, 2026)

### 🎉 Overview
PDFin is a modern, cross-platform desktop application for PDF manipulation built with JavaFX 21 and Apache PDFBox. This initial release provides essential PDF operations with a beautiful, intuitive user interface supporting multiple languages and themes.

---

## ✨ Features

### 🛠️ PDF Operations

#### 1. **Merge PDFs**
- Combine multiple PDF files into a single document
- Drag & drop support for easy file selection
- Reorder files before merging
- Visual file cards with thumbnails and metadata
- Duplicate detection and handling
- Persistent state across sessions

#### 2. **Split PDFs**
- Extract pages from PDF documents
- Multiple split modes:
  - Split by page ranges (e.g., 1-3, 5-7)
  - Extract specific pages
  - Split into individual pages
  - Split by page intervals
- Visual page thumbnails with zoom controls (1x - 2x)
- Real-time page preview
- State persistence across navigation

#### 3. **Compress PDFs**
- Reduce PDF file sizes while maintaining quality
- Three compression levels:
  - **Low** (300 DPI) - High quality, moderate compression
  - **Medium** (150 DPI) - Balanced quality and size
  - **High** (72 DPI) - Maximum compression
- Batch processing support
- Before/after size comparison
- Quality preview recommendations

#### 4. **Protect PDFs**
- Add password protection to PDF documents
- User password for document opening
- Owner password for permissions control
- Password strength indicator
- Visual feedback for security levels
- Batch protection support

### 🎨 User Interface

#### Modern Design
- **Two-pane layout** - Dedicated file workspace and settings panel
- **Drop zones** - Intuitive drag & drop areas
- **File cards** - Visual representation with thumbnails
- **Smooth animations** - Polished transitions and hover effects
- **Responsive design** - Adapts to different window sizes
- **Empty states** - Clear guidance when no files are selected

#### Theme Support
- **Dark Mode** - Easy on the eyes for extended use
- **Light Mode** - Clean and professional appearance
- Instant theme switching without restart
- Persistent theme preference
- Consistent styling across all components

#### Navigation
- **Sidebar navigation** - Quick access to all tools
- **Breadcrumb-style** back buttons
- **State preservation** - Settings persist across navigation
- **Recent files** - Quick access to recently processed files

### 🌍 Internationalization

#### Supported Languages
- **English** - Full translation
- **Bahasa Indonesia** - Complete Indonesian localization

#### i18n Features
- Real-time language switching
- No application restart required
- All UI elements translated
- Date/time formatting per locale
- Cultural number formatting

### 💡 User Experience

#### File Handling
- **Drag & Drop** - Add files by dragging from file explorer
- **Multi-select** - Select multiple files at once
- **File validation** - Automatic PDF format checking
- **Duplicate detection** - Prevents adding the same file twice
- **Thumbnail generation** - Visual file previews
- **Metadata extraction** - Display page count and file size

#### Progress & Feedback
- **Progress indicators** - Visual feedback during operations
- **Status messages** - Clear success/error notifications
- **Custom dialogs** - Themed, context-aware dialogs
- **Error handling** - Helpful error messages with solutions
- **Validation feedback** - Real-time input validation

#### Performance
- **Async operations** - Non-blocking UI during processing
- **Thumbnail caching** - Fast loading of previously seen files
- **Memory efficient** - Proper resource management
- **Responsive UI** - Smooth interactions even during heavy operations

---

## 🖥️ System Requirements

### Minimum Requirements
- **Operating System**: 
  - macOS 10.14 (Mojave) or later
  - Windows 10 (64-bit) or later
  - Linux (Ubuntu 20.04 or equivalent)
- **Java Runtime**: Java 21 or higher (included in standalone builds)
- **Memory**: 512 MB RAM minimum, 1 GB recommended
- **Disk Space**: 200 MB for installation
- **Display**: 1024x768 minimum resolution

### Recommended Requirements
- **Memory**: 2 GB RAM or more
- **Display**: 1920x1080 or higher
- **Java**: Latest Java 21 LTS version

---

## 📦 Installation

### Option 1: Download Pre-built JAR (Recommended)

1. **Download** the latest release:
   - Visit the [Releases](https://github.com/bicilique/PDFin/releases) page
   - Download `pdf-toolkit-1.0.0.jar`

2. **Run** the application:
   ```bash
   java -jar pdf-toolkit-1.0.0.jar
   ```

### Option 2: Build from Source

#### Prerequisites
- Java JDK 21 or higher
- Maven 3.8 or higher
- Git

#### Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/bicilique/PDFin.git
   cd PDFin
   ```

2. **Build with Maven**:
   ```bash
   mvn clean package
   ```

3. **Run the application**:
   ```bash
   java -jar target/pdf-toolkit-1.0.0.jar
   ```
   
   Or using Maven:
   ```bash
   mvn javafx:run
   ```

---

## 🚀 Quick Start Guide

### First Launch
1. Launch the application using one of the methods above
2. The home screen displays four main tools
3. Select a tool to begin

### Merging PDFs
1. Click **Merge** from the home screen
2. Add files:
   - Click **Add Files** button, or
   - Drag and drop PDF files into the drop zone
3. Reorder files if needed (drag file cards up/down)
4. Set output folder and filename
5. Click **Merge PDFs** button
6. View success dialog with options to open folder or process more files

### Splitting PDFs
1. Click **Split** from the home screen
2. Select a PDF file:
   - Click **Select File** button, or
   - Drag and drop a PDF file
3. Browse page thumbnails (use zoom slider if needed)
4. Enter page ranges or select split mode
5. Choose output folder
6. Click **Split** button

### Compressing PDFs
1. Click **Compress** from the home screen
2. Add one or more PDF files
3. Select compression level:
   - Low (300 DPI) for high quality
   - Medium (150 DPI) for balanced
   - High (72 DPI) for maximum compression
4. Set output folder
5. Click **Compress Now** button
6. Review file size reductions

### Protecting PDFs
1. Click **Protect** from the home screen
2. Add PDF files to protect
3. Enter password(s):
   - User password: Required to open the document
   - Owner password: Required to modify permissions
4. Check password strength indicator
5. Set output folder
6. Click **Protect PDFs** button

---

## 🧰 Technology Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| **Language** | Java | 21 LTS | Core programming language |
| **UI Framework** | JavaFX | 21 | Modern desktop UI |
| **PDF Library** | Apache PDFBox | 3.0.1 | PDF manipulation engine |
| **Build Tool** | Maven | 3.x | Dependency & build management |
| **Testing** | JUnit Jupiter | 5.10.1 | Unit testing framework |
| **UI Testing** | TestFX | 4.0.18 | JavaFX UI testing |
| **i18n** | ResourceBundle | Built-in | Internationalization |

---

## 📊 Project Statistics

- **Total Lines of Code**: ~12,000
- **Java Source Files**: 37
- **Test Files**: 18
- **Test Coverage**: 328 unit tests, 100% pass rate
- **Resource Files**: 46 (FXML, CSS, i18n, icons)
- **Supported Locales**: 2 (English, Indonesian)
- **Build Artifacts**: Single executable JAR (~30 MB)

---

## 🔧 Architecture & Design

### Design Patterns
- **MVC Pattern** - Separation of concerns (Controllers, Models, Views)
- **Observer Pattern** - Reactive UI with JavaFX properties
- **Service Layer** - Encapsulated business logic
- **Factory Pattern** - Icon and dialog creation
- **Singleton Pattern** - Theme and locale managers
- **State Pattern** - Application state management

### Key Components

#### Controllers
- `HomeController` - Landing page and navigation
- `MergeController` - PDF merging functionality
- `SplitController` - PDF splitting functionality
- `CompressController` - PDF compression functionality
- `ProtectController` - PDF protection functionality
- `ShellControllerNew` - Main application shell

#### Services
- `PdfMergeService` - PDF merging operations
- `PdfSplitService` - PDF splitting operations
- `CompressPdfService` - PDF compression operations
- `PdfProtectionService` - PDF encryption
- `PdfLockService` - Password protection
- `PdfThumbnailService` - Thumbnail generation
- `PdfPreviewService` - Page preview rendering
- `PdfThumbnailCache` - Thumbnail caching

#### Utilities
- `LocaleManager` - Language management
- `ThemeManager` - Theme switching
- `DefaultPaths` - Cross-platform path handling
- `AppPaths` - Application directory management
- `PageRangeParser` - Page range parsing
- `PdfMetadataUtil` - PDF metadata extraction

#### UI Components
- `CustomDialog` - Themed dialog system
- `TablerIconView` - Icon rendering
- `Icons` - Icon factory
- `PdfItemCell` - File list cell renderer
- `PdfFileCard` - File card component
- `PageThumbnailCard` - Page thumbnail component
- `RangeCard` - Range input component

---

## 🔒 Security Features

### Password Protection
- AES-256 encryption for user passwords
- AES-128 encryption for owner passwords
- Configurable permission settings
- Password strength validation
- Visual strength indicators

### File Handling
- Input validation for all file operations
- Path traversal prevention
- Secure temporary file handling
- Proper file cleanup after operations

---

## 🌐 Cross-Platform Compatibility

### Path Handling
- Uses `System.getProperty("user.home")` for user directories
- Platform-agnostic path construction with `Paths.get()`
- File separator handling via `File.separator`
- No hardcoded absolute paths in production code

### File Operations
- Java NIO for modern file operations
- Cross-platform directory creation
- Graceful fallbacks for permission issues
- Desktop integration via `java.awt.Desktop`

### UI Rendering
- JavaFX native rendering on all platforms
- Platform-specific JavaFX binaries included
- Consistent look and feel across operating systems
- Native file choosers and directory browsers

### Testing
- All 328 unit tests pass on macOS
- Platform-independent test code
- Temporary directory usage in tests
- Cross-platform compatibility verified

---

## 📝 Default Directories

### Application Data
- **Output Directory**: `${user.home}/PDFin`
  - macOS: `/Users/{username}/PDFin`
  - Windows: `C:\Users\{username}\PDFin`
  - Linux: `/home/{username}/PDFin`

### Preferences
- **macOS**: `~/Library/Preferences/com.pdftoolkit.preferences.plist`
- **Windows**: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\pdftoolkit`
- **Linux**: `~/.java/.userPrefs/com/pdftoolkit`

---

## 🐛 Known Issues

### Version 1.0.0
1. **Large PDF Files**: Splitting very large PDFs (1000+ pages) may consume significant memory
   - **Workaround**: Split in smaller batches
   
2. **Deprecated API Warning**: LocaleManager uses a deprecated Locale constructor
   - **Impact**: No functional impact, will be fixed in future release
   
3. **Thumbnail Generation**: Some complex PDFs may have slow thumbnail generation
   - **Workaround**: Thumbnails are cached after first load

---

## 🔮 Roadmap

### Version 1.1.0 (Planned)
- [ ] PDF rotation feature
- [ ] Page reordering within PDFs
- [ ] Watermark addition
- [ ] PDF to image conversion
- [ ] Batch rename functionality
- [ ] More language support (Spanish, French, German)

### Version 1.2.0 (Planned)
- [ ] PDF form filling
- [ ] Digital signature support
- [ ] OCR text extraction
- [ ] PDF comparison tool
- [ ] Cloud storage integration

### Version 2.0.0 (Future)
- [ ] Plugin system
- [ ] Custom compression profiles
- [ ] Batch processing scripts
- [ ] Command-line interface
- [ ] REST API for automation

---

## 🤝 Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### How to Contribute
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup
```bash
# Clone your fork
git clone https://github.com/yourusername/PDFin.git
cd PDFin

# Run tests
mvn test

# Run application in development
mvn javafx:run

# Build package
mvn clean package
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

### Libraries & Frameworks
- [Apache PDFBox](https://pdfbox.apache.org/) - PDF manipulation library
- [JavaFX](https://openjfx.io/) - Rich client platform
- [Tabler Icons](https://tabler-icons.io/) - Beautiful icon set

### Contributors
- Development Team - Initial release and core features
- QA Team - Comprehensive testing across platforms
- Design Team - Modern UI/UX design

---

## 📞 Support

### Getting Help
- **Issues**: Report bugs on [GitHub Issues](https://github.com/bicilique/PDFin/issues)
- **Discussions**: Join [GitHub Discussions](https://github.com/bicilique/PDFin/discussions)
- **Email**: support@pdftoolkit.com (if available)

### Reporting Bugs
When reporting bugs, please include:
1. Operating system and version
2. Java version (`java -version`)
3. Steps to reproduce
4. Expected vs actual behavior
5. Screenshots if applicable
6. Console error output

---

## 📈 Changelog

### [1.0.0] - 2026-01-31

#### Added
- Initial release with core PDF operations
- Merge, Split, Compress, and Protect functionality
- Modern two-pane UI design
- Dark and Light theme support
- English and Indonesian language support
- Drag & drop file handling
- Visual file cards with thumbnails
- Page preview with zoom controls
- Progress indicators and status feedback
- Custom themed dialogs
- State persistence across navigation
- Comprehensive error handling
- Cross-platform compatibility (macOS, Windows, Linux)
- 328 unit tests with 100% pass rate
- Complete documentation

#### Technical
- Java 21 LTS
- JavaFX 21
- Apache PDFBox 3.0.1
- Maven build system
- JUnit Jupiter 5.10.1
- TestFX 4.0.18
- Resource bundle i18n
- Single executable JAR distribution

---

## 🎯 Version Numbering

PDFin follows [Semantic Versioning](https://semver.org/):
- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality in a backward compatible manner
- **PATCH** version for backward compatible bug fixes

---

**Built with ❤️ by the PDFin Team**

*For more information, visit our [GitHub repository](https://github.com/bicilique/PDFin)*
