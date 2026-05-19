import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductListComponent } from './products/product-list.component';
import { CartService } from './cart/cart.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ProductListComponent],
  template: `
    <header>
      <h1>sjdscxZ Shop</h1>
      <div>Cart: {{ cart.count() }} items — {{ cart.total() | currency:'USD' }}</div>
    </header>
    <main>
      <app-product-list></app-product-list>
    </main>
  `,
  styles: [`
    :host { display: block; max-width: 1100px; margin: 2rem auto; padding: 0 1rem; font-family: sans-serif; }
    header { display: flex; justify-content: space-between; align-items: baseline; border-bottom: 1px solid #eee; padding-bottom: 1rem; margin-bottom: 1rem; }
  `]
})
export class AppComponent {
  constructor(public cart: CartService) {}
}
