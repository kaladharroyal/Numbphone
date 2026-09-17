package com.minimalphone.core.domain.focus

import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class FocusTickerState(
    val isSessionActive: Boolean = false,
    val session: FocusSession? = null,
    val remainingMillis: Long = 0L,
    val remainingSeconds: Long = 0L,
    val remainingMinutes: Long = 0L,
    val formattedTime: String = "00:00",
    val progress: Float = 0f,
    val isCompleted: Boolean = false
)

class GetFocusGoalsUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            FocusGoal(id = "preset_study", title = "Study & Exam Prep", category = "Study", isCustom = false),
            FocusGoal(id = "preset_dbms", title = "Finish DBMS Revision", category = "Study", isCustom = false),
            FocusGoal(id = "preset_coding", title = "Coding & Architecture", category = "Coding", isCustom = false),
            FocusGoal(id = "preset_reading", title = "Deep Reading", category = "Reading", isCustom = false),
            FocusGoal(id = "preset_deep_work", title = "Deep Work Sprint", category = "Work", isCustom = false),
            FocusGoal(id = "preset_sleep", title = "Rest & Sleep", category = "Sleep", isCustom = false)
        )
    }

    operator fun invoke(): Flow<List<FocusGoal>> {
        return focusSessionRepository.getRecentGoals().map { recents ->
            val customGoals = recents.filter { it.isCustom }
            (DEFAULT_PRESETS + customGoals).distinctBy { it.title.trim().lowercase() }
        }
    }
}

class StartFocusSessionUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(
        goal: FocusGoal,
        durationMinutes: Int,
        mode: FocusMode = FocusMode.STRICT
    ): Result<FocusSession> {
        val sanitizedDuration = durationMinutes.coerceIn(1, 720)
        val sanitizedTitle = goal.title.trim()

        if (sanitizedTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Focus goal cannot be empty."))
        }

        val startTime = System.currentTimeMillis()
        val durationMillis = sanitizedDuration * 60 * 1000L
        val endTime = startTime + durationMillis

        val session = FocusSession(
            id = UUID.randomUUID().toString(),
            startTime = startTime,
            endTime = endTime,
            durationMinutes = sanitizedDuration,
            goal = goal.copy(title = sanitizedTitle),
            mode = mode,
            status = SessionStatus.ACTIVE,
            bypassAttemptsCount = 0,
            exitFrictionSecondsCompleted = 0
        )

        focusSessionRepository.startSession(session)
        return Result.success(session)
    }
}

class GetActiveFocusSessionUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    operator fun invoke(): Flow<FocusSession?> {
        return focusSessionRepository.getActiveSession()
    }

    suspend fun getSync(): FocusSession? {
        return focusSessionRepository.getActiveSessionSync()
    }
}

class ObserveFocusSessionTickerUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    operator fun invoke(): Flow<FocusTickerState> {
        return focusSessionRepository.getActiveSession().flatMapLatest { session ->
            flow {
                if (session == null || !session.isCurrentlyActive) {
                    emit(FocusTickerState(isSessionActive = false))
                } else {
                    val totalDurationMillis = (session.durationMinutes * 60 * 1000L).coerceAtLeast(1000L)

                    while (true) {
                        val now = System.currentTimeMillis()
                        val remainingMillis = (session.endTime - now).coerceAtLeast(0L)
                        val remainingSeconds = remainingMillis / 1000L
                        val remainingMinutes = remainingSeconds / 60L
                        val elapsedMillis = (now - session.startTime).coerceAtLeast(0L)
                        val progress = (elapsedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)

                        val hours = remainingSeconds / 3600
                        val minutes = (remainingSeconds % 3600) / 60
                        val seconds = remainingSeconds % 60

                        val formatted = if (hours > 0) {
                            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        }

                        val isCompleted = remainingMillis <= 0L
                        emit(
                            FocusTickerState(
                                isSessionActive = !isCompleted,
                                session = session,
                                remainingMillis = remainingMillis,
                                remainingSeconds = remainingSeconds,
                                remainingMinutes = remainingMinutes,
                                formattedTime = formatted,
                                progress = progress,
                                isCompleted = isCompleted
                            )
                        )

                        if (isCompleted) {
                            focusSessionRepository.completeSession(session.id)
                            break
                        }

                        delay(1000L)
                    }
                }
            }
        }
    }
}

class CompleteFocusSessionUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(sessionId: String) {
        focusSessionRepository.completeSession(sessionId)
    }
}

class CancelFocusSessionUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(sessionId: String, frictionSeconds: Int = 0, reason: String? = null) {
        focusSessionRepository.cancelSession(sessionId, frictionSeconds, reason)
    }
}
