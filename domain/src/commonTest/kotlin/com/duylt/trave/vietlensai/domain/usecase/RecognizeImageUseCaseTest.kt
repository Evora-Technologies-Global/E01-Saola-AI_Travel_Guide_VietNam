package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** imagePath, mode and the location that actually reached the repository. */
private typealias RecordedCall = Triple<String, LensMode, GeoPoint?>

/**
 * Location handling is the whole reason this use case exists, so that is what is
 * pinned down here: it must sharpen recognition when available and never block it.
 *
 * The granted permission is the only gate. An app-level preference used to sit in
 * front of it and defaulted to on while the permission was never requested
 * anywhere, so captures were written with null coordinates and the passport map
 * stayed empty however far the traveller went.
 *
 * The doubles are hand-written rather than mocked: this suite runs on iOS as well
 * as the JVM now, and MockK is JVM-only. Recording the arguments by hand also
 * makes the assertion that matters here — *what location reached the repository* —
 * a plain equality check instead of a verification DSL.
 */
class RecognizeImageUseCaseTest {

    private class FakeDiscoveryRepository : DiscoveryRepository {
        var result: AppResult<Discovery> = AppResult.Failure(AppError.Unexpected("not stubbed"))
        val calls = mutableListOf<RecordedCall>()

        override suspend fun recognize(
            imagePath: String,
            mode: LensMode,
            location: GeoPoint?,
        ): AppResult<Discovery> {
            calls += Triple(imagePath, mode, location)
            return result
        }

        override fun observeDiscoveries(): Flow<List<Discovery>> = flowOf(emptyList())
        override fun observeFavorites(): Flow<List<Discovery>> = flowOf(emptyList())
        override fun observeDiscovery(id: String): Flow<Discovery?> = flowOf(null)
        override fun observeByProvince(provinceId: String): Flow<List<Discovery>> = flowOf(emptyList())
        override suspend fun getDiscovery(id: String): Discovery? = null
        override suspend fun toggleFavorite(id: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun delete(id: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteAll(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeLocationRepository : LocationRepository {
        var permission = false
        var fix: AppResult<GeoPoint> = AppResult.Failure(AppError.LocationUnavailable)
        var currentLocationCalls = 0

        override suspend fun currentLocation(): AppResult<GeoPoint> {
            currentLocationCalls++
            return fix
        }

        override fun hasLocationPermission(): Boolean = permission
    }

    private val discoveryRepository = FakeDiscoveryRepository()
    private val locationRepository = FakeLocationRepository()

    private val useCase = RecognizeImageUseCase(
        discoveryRepository = discoveryRepository,
        locationRepository = locationRepository,
    )

    private val discovery = Discovery(
        id = "id",
        title = "Temple of Literature",
        localName = null,
        category = DiscoveryCategory.LANDMARK,
        imagePath = null,
        summary = "",
        sections = emptyList(),
        funFacts = emptyList(),
        tags = emptyList(),
        nearbySuggestions = emptyList(),
        suggestedQuestions = emptyList(),
        confidence = 0.9f,
        location = null,
        placeHint = null,
        isFavorite = false,
        modelUsed = null,
        createdAt = Instant.fromEpochSeconds(0),
    )

    @Test
    fun `attaches the current location when the permission is granted`() = runTest {
        val fix = GeoPoint(21.0287, 105.8524)
        locationRepository.permission = true
        locationRepository.fix = AppResult.Success(fix)
        discoveryRepository.result = AppResult.Success(discovery)

        val result = useCase("/tmp/photo.jpg", LensMode.LANDMARK)

        assertTrue(result is AppResult.Success)
        assertEquals<List<RecordedCall>>(
            listOf(Triple("/tmp/photo.jpg", LensMode.LANDMARK, fix)),
            discoveryRepository.calls,
        )
    }

    /**
     * Recognising Hạ Long Bay without coordinates is still worth doing, so a failed
     * fix must degrade the prompt rather than fail the capture.
     */
    @Test
    fun `still recognises when no location fix is available`() = runTest {
        locationRepository.permission = true
        locationRepository.fix = AppResult.Failure(AppError.LocationUnavailable)
        discoveryRepository.result = AppResult.Success(discovery)

        val result = useCase("/tmp/photo.jpg", LensMode.AUTO)

        assertTrue(result is AppResult.Success)
        assertEquals<List<RecordedCall>>(
            listOf(Triple("/tmp/photo.jpg", LensMode.AUTO, null)),
            discoveryRepository.calls,
        )
    }

    @Test
    fun `skips the location fix when permission was never granted`() = runTest {
        locationRepository.permission = false
        discoveryRepository.result = AppResult.Success(discovery)

        useCase("/tmp/photo.jpg", LensMode.FOOD)

        assertEquals(0, locationRepository.currentLocationCalls)
        assertEquals<List<RecordedCall>>(
            listOf(Triple("/tmp/photo.jpg", LensMode.FOOD, null)),
            discoveryRepository.calls,
        )
    }

    @Test
    fun `passes a recognition failure straight through`() = runTest {
        locationRepository.permission = false
        discoveryRepository.result = AppResult.Failure(AppError.NoConnection)

        val result = useCase("/tmp/photo.jpg", LensMode.AUTO)

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
    }
}
