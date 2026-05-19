import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl?: string;
}

export interface CheckoutItem { productId: number; quantity: number; }
export interface CheckoutRequest { email: string; items: CheckoutItem[]; }
export interface OrderLine { id: number; productId: number; productName: string; quantity: number; unitPrice: number; }
export interface CustomerOrder {
  id: number;
  customerEmail: string;
  total: number;
  status: string;
  createdAt: string;
  lines: OrderLine[];
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private base = environment.apiBase;

  constructor(private http: HttpClient) {}

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/api/products`);
  }

  get(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/api/products/${id}`);
  }

  checkout(req: CheckoutRequest): Observable<CustomerOrder> {
    return this.http.post<CustomerOrder>(`${this.base}/api/orders/checkout`, req);
  }

  ordersByEmail(email: string): Observable<CustomerOrder[]> {
    return this.http.get<CustomerOrder[]>(`${this.base}/api/orders/by-email`, { params: { email } });
  }
}
