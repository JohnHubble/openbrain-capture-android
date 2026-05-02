package com.hubble.openbrain.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hubble.openbrain.R
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.CaptureStateHolder
import com.hubble.openbrain.tile.ToggleCaptureReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Home-screen widget that toggles capture with one tap and shows current phase.
 * Reads in-memory [CaptureStateHolder] via a Hilt EntryPoint because AppWidgetProvider
 * cannot itself be @AndroidEntryPoint.
 */
class CaptureWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StateEntryPoint {
        fun state(): CaptureStateHolder
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val phase = EntryPointAccessors
            .fromApplication(context.applicationContext, StateEntryPoint::class.java)
            .state()
            .state.value.phase
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, phase))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CaptureWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.hubble.openbrain.action.WIDGET_REFRESH"

        fun refresh(context: Context) {
            context.sendBroadcast(
                Intent(context, CaptureWidgetProvider::class.java)
                    .setAction(ACTION_WIDGET_REFRESH),
            )
        }

        private fun buildRemoteViews(context: Context, phase: CapturePhase): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_capture)
            val label = when (phase) {
                is CapturePhase.Recording -> "Recording · tap to stop"
                CapturePhase.Transcribing -> "Transcribing…"
                CapturePhase.Saving -> "Saving…"
                is CapturePhase.Preview -> "Awaiting save"
                else -> "Tap to capture"
            }
            views.setTextViewText(R.id.widget_label, label)
            views.setTextViewText(R.id.widget_title, "Open Brain")
            val toggleIntent = Intent(context, ToggleCaptureReceiver::class.java)
                .setAction(ToggleCaptureReceiver.ACTION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getBroadcast(context, 0, toggleIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }
    }
}
