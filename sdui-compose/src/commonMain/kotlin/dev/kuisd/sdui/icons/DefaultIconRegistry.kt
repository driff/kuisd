package dev.kuisd.sdui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings

/**
 * Set base de iconos Material expuesto por el motor. La app extiende con `+` cualquier icono
 * propio (`DefaultIconRegistry + iconRegistry { register("brand.logo", myVector) }`).
 */
val DefaultIconRegistry: IconRegistry = iconRegistry {
    register("home", Icons.Filled.Home)
    register("search", Icons.Filled.Search)
    register("settings", Icons.Filled.Settings)
    register("add", Icons.Filled.Add)
    register("edit", Icons.Filled.Edit)
    register("delete", Icons.Filled.Delete)
    register("close", Icons.Filled.Close)
    register("check", Icons.Filled.Check)
    register("arrowBack", Icons.AutoMirrored.Filled.ArrowBack)
    register("arrowForward", Icons.AutoMirrored.Filled.ArrowForward)
    register("favorite", Icons.Filled.Favorite)
    register("moreVert", Icons.Filled.MoreVert)
}
