import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService, Product } from '../core/product.service';
import { CartService } from '../cart/cart.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Products</h2>
    @if (loading()) {
      <p>Loading…</p>
    } @else {
      <div class="grid">
        @for (p of products(); track p.id) {
          <div class="card">
            <h3>{{ p.name }}</h3>
            <p>{{ p.description }}</p>
            <strong>{{ p.price | currency:'USD' }}</strong>
            <small>{{ p.stock }} in stock</small>
            <button (click)="cart.add(p)" [disabled]="p.stock === 0">Add to cart</button>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; }
    .card { border: 1px solid #ddd; padding: 1rem; border-radius: 8px; }
    button { margin-top: .5rem; }
  `]
})
export class ProductListComponent implements OnInit {
  products = signal<Product[]>([]);
  loading = signal(true);

  constructor(private svc: ProductService, public cart: CartService) {}

  ngOnInit() {
    this.svc.list().subscribe({
      next: (list) => { this.products.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }
}
