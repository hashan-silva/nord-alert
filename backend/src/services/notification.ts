import admin from 'firebase-admin';
import { Alert } from '../models/alert';

if (!admin.apps.length) {
  admin.initializeApp();
}

/**
 * Send a push notification for a given alert to a FCM topic.
 */
export async function sendAlertNotification(alert: Alert, topic: string) {
  await admin.messaging().send({
    topic,
    notification: {
      title: alert.headline,
      body: alert.description ?? '',
    },
    data: {
      id: alert.id,
      url: alert.url,
      source: alert.source,
      severity: alert.severity,
    },
  });
}
