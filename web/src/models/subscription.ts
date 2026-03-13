export interface CreateSubscriptionRequest {
  counties: string[];
  email: string;
  severity: string;
  sources: string[];
}

export interface SubscriptionItem {
  counties: string[];
  createdAt: string;
  email: string;
  id: string;
  lastNotifiedAt?: string;
  severity?: string;
  sources: string[];
}
