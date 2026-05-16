package com.movie.app.best.ui.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.movie.app.best.ui.screens.categories.CategoriesScreen
import com.movie.app.best.ui.screens.categories.CategoryPageScreen
import com.movie.app.best.ui.screens.downloads.DownloadsScreen
import com.movie.app.best.ui.screens.downloads.LocalVideoScreen
import com.movie.app.best.ui.screens.home.HomeScreen
import com.movie.app.best.ui.screens.library.LibraryScreen
import com.movie.app.best.ui.screens.moviedetail.MovieDetailScreen
import com.movie.app.best.ui.screens.search.SearchScreen
import com.movie.app.best.ui.screens.tvshowdetail.TVShowDetailScreen
import com.movie.app.best.ui.screens.tvshows.TVShowsScreen
import java.net.URLEncoder
import com.movie.app.best.ui.screens.player.VideoPlayerScreen
import com.movie.app.best.ui.screens.movies.MoviesScreen
import com.movie.app.best.ui.screens.settings.SettingsScreen
import com.movie.app.best.ui.screens.trending.TrendingScreen
import com.movie.app.best.ui.screens.auth.LoginScreen
import com.movie.app.best.ui.screens.profile.ProfileScreen
import com.movie.app.best.ui.screens.notification.NotificationScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Movies : Screen("movies")
    object Trending : Screen("trending")
    object Categories : Screen("categories")
    object CategoryPage : Screen("category/{categorySlug}/{categoryName}") {
        fun createRoute(slug: String, name: String): String {
            return "category/${Uri.encode(slug)}/${Uri.encode(name)}"
        }
    }
    object MovieDetail : Screen("movie/{slug}") {
        fun createRoute(slug: String) = "movie/${Uri.encode(slug)}"
    }
    object Search : Screen("search")
    object TVShows : Screen("tv-shows")
    object Downloads : Screen("downloads")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object LocalVideos : Screen("localVideos")
    object Notifications : Screen("notifications")
    object SeriesDetail : Screen("series/{slug}") {
        fun createRoute(slug: String) = "series/${Uri.encode(slug)}"
    }
    object VideoPlayer : Screen("videoPlayer?playerUrl={playerUrl}&streamUrl={streamUrl}&title={title}&youtubeId={youtubeId}&movieId={movieId}") {
        fun createRoute(playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String = ""): String {
            return "videoPlayer?playerUrl=${URLEncoder.encode(playerUrl, "UTF-8")}&streamUrl=${URLEncoder.encode(streamUrl, "UTF-8")}&title=${URLEncoder.encode(title, "UTF-8")}&youtubeId=${URLEncoder.encode(youtubeId, "UTF-8")}&movieId=${URLEncoder.encode(movieId, "UTF-8")}"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun navigateToContent(slug: String, isSeries: Boolean) {
        if (isSeries) {
            navController.navigate(Screen.SeriesDetail.createRoute(slug))
        } else {
            navController.navigate(Screen.MovieDetail.createRoute(slug))
        }
    }

    val tabRoutes = listOf(
        Screen.Home.route,
        Screen.Trending.route,
        Screen.Library.route,
        Screen.Downloads.route,
        Screen.Profile.route
    )

    fun slideDirection(from: String?, to: String?): Int {
        val fromIdx = tabRoutes.indexOf(from)
        val toIdx = tabRoutes.indexOf(to)
        if (fromIdx < 0 || toIdx < 0) return 0
        return if (toIdx > fromIdx) 1 else -1
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            val dir = slideDirection(initialState.destination.route, targetState.destination.route)
            if (dir != 0) {
                slideInHorizontally(
                    initialOffsetX = { w -> dir * w / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    initialOffsetX = { w -> w },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        exitTransition = {
            val dir = slideDirection(initialState.destination.route, targetState.destination.route)
            if (dir != 0) {
                slideOutHorizontally(
                    targetOffsetX = { w -> -dir * w / 4 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(150))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { w -> -w / 4 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(150))
            }
        },
        popEnterTransition = {
            val dir = slideDirection(targetState.destination.route, initialState.destination.route)
            if (dir != 0) {
                slideInHorizontally(
                    initialOffsetX = { w -> dir * w / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    initialOffsetX = { w -> -w / 4 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        popExitTransition = {
            val dir = slideDirection(targetState.destination.route, initialState.destination.route)
            if (dir != 0) {
                slideOutHorizontally(
                    targetOffsetX = { w -> -dir * w / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(150))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { w -> w },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(150))
            }
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onDownloadClick = { navController.navigate(Screen.Downloads.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            MovieDetailScreen(
                slug = slug,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { playerUrl, streamUrl, title, youtubeId, movieId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(playerUrl, streamUrl, title, youtubeId, movieId))
                },                onSeriesClick = { seriesSlug ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesSlug)) {
                        popUpTo(Screen.MovieDetail.route) { inclusive = true }
                    }
                },
                onDownloadClick = { _, _ -> }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            TVShowDetailScreen(
                slug = slug,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { playerUrl, streamUrl, title, youtubeId, movieId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(playerUrl, streamUrl, title, youtubeId, movieId))
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) }
            )
        }

        composable(Screen.TVShows.route) {
            TVShowsScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onCategoryClick = { slug, name ->
                    navController.navigate(Screen.CategoryPage.createRoute(slug, name))
                },
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(
            route = Screen.CategoryPage.route,
            arguments = listOf(
                navArgument("categorySlug") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("categorySlug") ?: ""
            val name = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryPageScreen(
                categorySlug = slug,
                categoryName = name,
                onContentClick = { s, isSeries -> navigateToContent(s, isSeries) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("playerUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("streamUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("youtubeId") { type = NavType.StringType; defaultValue = "" },
                navArgument("movieId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val playerUrl = backStackEntry.arguments?.getString("playerUrl") ?: ""
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val youtubeId = backStackEntry.arguments?.getString("youtubeId") ?: ""
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""

            VideoPlayerScreen(
                onBackClick = { navController.popBackStack() },
                playerUrl = playerUrl,
                streamUrl = streamUrl,
                title = title,
                youtubeId = youtubeId,
                movieId = movieId
            )
        }

        composable(Screen.Movies.route) {
            MoviesScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(Screen.Trending.route) {
            TrendingScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onLocalVideosClick = { navController.navigate(Screen.LocalVideos.route) },
                onPlayFile = { path, name ->
                    navController.navigate(Screen.VideoPlayer.createRoute("", path, name, "", ""))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onBookmarksClick = { navController.navigate(Screen.Library.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.LocalVideos.route) {
            LocalVideoScreen(
                onBackClick = { navController.popBackStack() },
                onPlayVideo = { path, title ->
                    navController.navigate(Screen.VideoPlayer.createRoute("", "file://$path", title, "", ""))
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
