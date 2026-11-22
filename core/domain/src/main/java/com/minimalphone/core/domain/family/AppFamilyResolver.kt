package com.minimalphone.core.domain.family

import android.content.Context
import com.minimalphone.core.model.AppFamily
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFamilyResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val families: List<AppFamily> by lazy { loadFamilies() }

    private val packageToFamily: Map<String, String> by lazy {
        families.flatMap { f -> f.packages.map { pkg -> pkg to f.familyId } }.toMap()
    }

    fun getFamilyId(packageName: String): String? = packageToFamily[packageName]

    fun getFamily(familyId: String): AppFamily? = families.find { it.familyId == familyId }

    fun getFamilyMembers(packageName: String): List<String> {
        val fid = getFamilyId(packageName) ?: return emptyList()
        return getFamily(fid)?.packages ?: emptyList()
    }

    fun getAllFamilies(): List<AppFamily> = families

    private fun loadFamilies(): List<AppFamily> {
        return try {
            val json = context.assets.open("app_families.json").bufferedReader().readText()
            val root = JSONObject(json)
            val arr = root.getJSONArray("families")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val pkgArr = obj.getJSONArray("packages")
                AppFamily(
                    familyId = obj.getString("familyId"),
                    packages = (0 until pkgArr.length()).map { j -> pkgArr.getString(j) }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
