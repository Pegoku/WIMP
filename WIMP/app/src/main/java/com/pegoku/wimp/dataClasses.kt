package com.pegoku.wimp
data class TrackerCreateBody(
    val trackingNumber: String,
    val courierCode: String,
    val destinationPostCode: String? = null
)

data class TrackingRequestBody(
    val trackingNumber: String,
    val courierCode: String? = null,
    val destinationPostCode: String? = null
)

data class TrackerCreateResponse(
    val data: TrackerCreateDataData
)

data class TrackerCreateDataData(
    val tracker: TrackerCreateTrackerData
)

data class TrackerCreateTrackerData(
    val trackerId: String,
    val trackingNumber: String,
    val shipmentReference: String? = null,
    val courierCode: List<String>,
    val clientTrackerId: String? = null,
    val isSubscribed: Boolean,
    val isTracked: Boolean,
    val createdAt: String
)

data class CouriersResponse(
    val data: CouriersData
)

data class TrackingResponse(
    val data: TrackingData
)

data class CouriersData(
    val couriers: List<Couriers>
)

data class TrackingData(
    val trackings: List<TrackingDataInner>
)

data class TrackingDataInner(
    val tracker: TrackerData,
    val shipment: ShipmentData,
    val events: List<TrackingEvent>,
    val statistics: StatisticsData
)

data class TrackerData(
    val trackerId: String,
    val trackingNumber: String,
    val shipmentReference: String? = null,
    val courierCode: List<String>,
    val clientTrackerId: String? = null,
    val isSubscribed: Boolean,
    val isTracked: Boolean,
    val createdAt: String
)

data class ShipmentData(
    val shipmentId: String,
    val statusCode: String,
    val statusCategory: String,
    val statusMilestone: String,
    val originCountryCode: String? = null,
    val destinationCountryCode: String? = null,
    val delivery: DeliveryData,
    val trackingNumbers: List<TrackingNumbersData>,
    val recipient: RecipientData
)

data class TrackingNumbersData(
    val tn: String
)

data class DeliveryData(
    val estimatedDeliveryDate: String? = null,
    val service: String? = null,
    val signedBy: String? = null
)

data class RecipientData(
    val name: String? = null,
    val address: String? = null,
    val postCode: String? = null,
    val city: String? = null,
    val subdivision: String? = null
)

data class TrackingEvent(
    val eventId: String,
    val trackingNumber: String,
    val eventTrackingNumber: String,
    val status: String,
    val occurrenceDatetime: String,
    val order: String? = null,
    val datetime: String,
    val hasNoTime: Boolean,
    val utcOffset: String? = null,
    val location: String? = null,
    val sourceCode: String? = null,
    val courierCode: String? = null,
    val statusCode: String? = null,
    val statusCategory: String? = null,
    val statusMilestone: String? = null,
)


data class StatisticsData(
    val timestamps: TimestampsData
)

data class TimestampsData(
    val infoReceivedDatetime: String? = null,
    val inTransitDatetime: String? = null,
    val outForDeliveryDatetime: String? = null,
    val failedAttemptDatetime: String? = null,
    val availableForPickupDatetime: String? = null,
    val exceptionDatetime: String? = null,
    val deliveredDatetime: String? = null,
)

data class Couriers(
    val courierCode: String,
    val courierName: String,
    val website: String,
    val isPost: Boolean,
    val countryCode: String,
    val requiredFields: List<String>? = emptyList(),
    val isDeprecated: Boolean
)
