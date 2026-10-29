package co.candyhouse.app.homewidget

object WidgetCommandPolicy {
    fun allows(widgetExists: Boolean, savedUuid: String?, requestedUuid: String?, operation: String?): Boolean =
        widgetExists && !savedUuid.isNullOrBlank() && savedUuid.equals(requestedUuid, true) && operation == "toggle"
}
