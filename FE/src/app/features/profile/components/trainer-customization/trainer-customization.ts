import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';

type Category = 'clothes' | 'accessory' | 'pose' | 'background';
import { SkinItem } from '../../data-access/profile-api.types';

@Component({
  selector: 'app-trainer-customization',
  imports: [],
  templateUrl: './trainer-customization.html',
  styleUrl: './trainer-customization.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerCustomization {
  readonly items = input.required<SkinItem[]>();
  readonly selectedId = input<string | null>(null);
  readonly selectItem = output<string>();

  protected readonly activeTab = signal<Category>('clothes');

  protected readonly tabs: { key: Category; label: string }[] = [
    { key: 'clothes', label: 'ROPA' },
    { key: 'accessory', label: 'ACCESORIOS' },
    { key: 'pose', label: 'POSE' },
    { key: 'background', label: 'FONDO' },
  ];

  protected get filteredItems(): SkinItem[] {
    return this.items().filter((item) => item.category === this.activeTab());
  }

  protected setTab(tab: Category): void {
    this.activeTab.set(tab);
  }
}
