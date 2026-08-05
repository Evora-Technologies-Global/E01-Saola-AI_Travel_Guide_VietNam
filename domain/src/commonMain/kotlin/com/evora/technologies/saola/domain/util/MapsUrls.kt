package com.evora.technologies.saola.domain.util

import com.evora.technologies.saola.domain.model.NearbyPlace

/**
 * The URL that hands a place over to Google Maps with the destination already filled in.
 *
 * Google's cross-platform Maps URL scheme rather than a platform intent or a
 * `comgooglemaps://` link. On Android it resolves to the Maps app through its own
 * intent filter; on iOS it opens the Maps app if installed and Safari if not, with no
 * `LSApplicationQueriesSchemes` entry needed and no silent failure when the app is
 * absent. One string covers both platforms, which is why this can live in `:domain`
 * at all. It needs no API key — this is a plain web link, not an API call.
 *
 * The origin is deliberately left out. Omitting it makes Maps use the device's own
 * current position, which is both fresher than the fix this app is holding and correct
 * if the traveller has walked on since the search.
 *
 * The destination is coordinates, with the name carried alongside only as a label. The
 * places on this screen come from OpenStreetMap and have no Google place id to pin
 * them by, and a name alone would be resolved by a Maps *search* — which is how you end
 * up navigating to a different café of the same name three districts away.
 */
fun directionsUrl(place: NearbyPlace, mode: TravelMode = TravelMode.WALKING): String =
    buildString {
        append("https://www.google.com/maps/dir/?api=1")
        append("&destination=")
        append(place.location.latitude)
        append(',')
        append(place.location.longitude)
        append("&travelmode=")
        append(mode.wireName)
    }

/**
 * How Google Maps should plan the route.
 *
 * Walking is the default, unlike a general-purpose maps link. Everything this screen
 * offers is inside five kilometres and most of it inside one, and the traveller it is
 * written for is on foot in a city centre — routing them by car through Hanoi's old
 * quarter would be the wrong answer to a question about a temple two streets away.
 */
enum class TravelMode(val wireName: String) {
    WALKING("walking"),
    DRIVING("driving"),
    TWO_WHEELER("two_wheeler"),
    TRANSIT("transit"),
}
