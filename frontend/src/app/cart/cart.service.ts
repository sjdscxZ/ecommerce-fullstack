import { Injectable, signal, computed } from '@angular/core';
import { Product } from '../core/product.service';

export interface CartLine { product: Product; quantity: number; }

@Injectable({ providedIn: 'root' })
export class CartService {
  private lines = signal<CartLine[]>([]);

  readonly items = this.lines.asReadonly();
  readonly count = computed(() => this.lines().reduce((acc, l) => acc + l.quantity, 0));
  readonly total = computed(() => this.lines().reduce((acc, l) => acc + l.quantity * l.product.price, 0));

  add(product: Product) {
    const current = this.lines();
    const existing = current.find(l => l.product.id === product.id);
    if (existing) {
      this.lines.set(current.map(l =>
        l.product.id === product.id ? { ...l, quantity: l.quantity + 1 } : l));
    } else {
      this.lines.set([...current, { product, quantity: 1 }]);
    }
  }

  remove(productId: number) {
    this.lines.set(this.lines().filter(l => l.product.id !== productId));
  }

  clear() { this.lines.set([]); }
}
