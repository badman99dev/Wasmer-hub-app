# Movie App

> **EDUCATIONAL PROJECT**: This application was created for learning purposes only and is not intended for commercial distribution. It serves as a practical exercise for Android development with Jetpack Compose, API integration, and modern mobile architecture patterns.

A modern Android streaming application built with Jetpack Compose that allows users to browse and watch movies and TV shows with a Netflix-like experience.

![App Screenshot](https://via.placeholder.com/800x400?text=Movie+App+Screenshot)

## Learning Objectives

This project was developed to demonstrate and practice:
- Jetpack Compose UI development
- MVVM architecture implementation
- API integration with Retrofit
- Dependency injection with Hilt
- Navigation in Compose applications
- Handling media content in Android
- Modern app architecture patterns

## Features

- **Modern UI**: Built entirely with Jetpack Compose for a fluid and responsive interface
- **Movies & TV Shows**: Browse popular movies and TV shows with detailed information
- **Video Playback**: Watch trailers directly in the app and full content in browser
- **Show Details**: View comprehensive information about TV shows including seasons and episodes
- **Search Functionality**: Find your favorite movies and TV shows easily
- **Trending Content**: Stay updated with what's popular right now
- **Responsive Design**: Optimized for various screen sizes and orientations
- **Smooth Navigation**: Bottom navigation bar for easy access to main sections

## Current Limitations

- **External Browser Playback**: Currently, movie and TV show content is played by redirecting to an external browser due to WebView limitations with certain streaming services. Only trailers play directly in the app.
- **API Rate Limiting**: The app uses TMDb API which has rate limits for free accounts.
- **Offline Support**: Offline mode is planned but not yet implemented.

## Tech Stack

- **100% Kotlin**: Modern, concise and safe programming language
- **Jetpack Compose**: Declarative UI toolkit for native Android UI
- **Material Design 3**: Latest Material design components and theming
- **MVVM Architecture**: Clean separation of UI, business logic, and data
- **Coroutines & Flow**: Asynchronous programming with sequential code
- **Retrofit & OkHttp**: Type-safe HTTP client for API calls
- **Hilt**: Dependency injection for better testability and maintainability
- **Coil**: Image loading library optimized for Compose
- **Navigation Compose**: Handle app navigation and deep links

## Project Components

### Android App
The primary client application built with Jetpack Compose and modern Android development practices.

### Website Backend
A Next.js web application that serves as both:
- A web frontend for users accessing via browsers
- An API proxy/backend for the Android app, handling TMDb API interactions

## Screens

### Splash Screen
A visually appealing introduction screen displaying the app's logo with smooth fade-in animation.

### Home
The main landing page featuring highlighted and curated content.

### Movies
Browse through a collection of movies with filtering options.

### TV Shows
Explore TV series with season and episode details.

### TV Show Detail
Comprehensive view of a TV show including:
- Show information (rating, genre, year)
- Season selection
- Episode lists with descriptions
- Cast information
- Similar shows recommendations

### Movie Detail
Detailed information about a movie including:
- Movie metadata (rating, runtime, release date)
- Cast information
- Similar movie recommendations
- Play movie option

### Video Player
Watch trailers directly in the app with a clean interface:
- YouTube integration for trailers
- External browser launch for full content (current solution)
- Fullscreen support

### Search
Find specific content with real-time search results.

### Trending
Discover what's popular right now across movies and TV shows.

## Architecture

The application follows the MVVM (Model-View-ViewModel) architecture pattern with a clean organization:

```
app/
└── src/main/
    ├── java/com/movie/app/best/
    │   ├── api/                # API service interfaces and data models
    │   ├── models/             # Domain models used throughout the app
    │   ├── repository/         # Data repositories connecting API to UI
    │   ├── ui/
    │   │   ├── components/     # Reusable UI components
    │   │   ├── navigation/     # Navigation handling and routes
    │   │   ├── screens/        # Individual app screens
    │   │   │   ├── home/       # Home screen components
    │   │   │   ├── main/       # Main container screen
    │   │   │   ├── movies/     # Movies screen
    │   │   │   ├── player/     # Video player screen
    │   │   │   ├── search/     # Search functionality
    │   │   │   ├── splash/     # Splash screen
    │   │   │   ├── tvshowdetail/ # TV show details
    │   │   │   └── tvshows/    # TV shows screen
    │   │   └── theme/          # App theming
    │   ├── utils/              # Utility functions and extensions
    │   └── MainActivity.kt     # Main entry point
    └── res/                    # App resources (drawables, values, etc.)
```

## TODO List

### High Priority
- [ ] Fix WebView playback issues to enable in-app content viewing
- [ ] Implement caching for API responses to reduce data usage
- [ ] Add error handling for network failures
- [ ] Implement pagination for movie and TV show listings
- [ ] Add user settings screen for preferences

### Medium Priority
- [ ] Implement offline mode with local database storage
- [ ] Add user watchlist functionality
- [ ] Improve video player controls and experience
- [ ] Optimize image loading and caching
- [ ] Add dark/light theme toggle

### Low Priority
- [ ] Implement analytics for user behavior tracking
- [ ] Add content filtering options
- [ ] Support for different languages
- [ ] Add animation transitions between screens
- [ ] Implement widget for home screen

## Installation

1. Clone the repository
```bash
git clone https://github.com/springmusk026/MovieApp
```

2. Open the project in Android Studio
3. Sync project with Gradle files
4. Run the app on an emulator or physical device

## Requirements

- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35
- Kotlin 1.9.0 or higher

## Configuration

The app communicates with a Next.js backend that serves as a proxy for TMDb (The Movie Database) API. 

### API Configuration
The base URL for the backend API is configured in the app's build.gradle.kts file:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"https://your-website-domain.com\"")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://your-website-domain.com\"")
    }
}
```

Simply replace "your-website-domain.com" with the actual domain of your backend service. No direct TMDb API key is needed in the Android app itself, as authentication is handled by the backend service.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

> **Educational Disclaimer**: This project uses TMDb API and references to streaming services for educational purposes only. All content remains the property of their respective owners, and this project is not meant for distribution or commercial use.

## Acknowledgments

- TMDb API for providing movie and TV show data
- Material Design guidelines for UI inspiration
- Open source libraries that made this project possible
- Educational resources and tutorials that helped in learning Jetpack Compose and modern Android development
- Fellow learners and mentors who provided feedback and guidance 