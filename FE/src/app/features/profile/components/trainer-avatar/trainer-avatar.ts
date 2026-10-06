import { ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild, afterNextRender, effect, OnDestroy } from '@angular/core';
import { TrainerSkin } from '../../data-access/profile-api.types';
import { buildPalette, generateTrainerSprite, TrainerSpriteConfig } from '../../sprite-system/trainer-sprite';

@Component({
  selector: 'app-trainer-avatar',
  imports: [],
  templateUrl: './trainer-avatar.html',
  styleUrl: './trainer-avatar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerAvatar implements OnDestroy {
  readonly skin = input.required<TrainerSkin>();
  readonly skinIndex = input.required<number>();
  readonly totalSkins = input.required<number>();
  readonly prev = output<void>();
  readonly next = output<void>();

  protected readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('spriteCanvas');
  protected readonly shadowCanvas = viewChild<ElementRef<HTMLCanvasElement>>('shadowCanvas');

  private frame = 0;
  private animId: number | null = null;

  private readonly SCALE = 4;

  constructor() {
    afterNextRender(() => {
      this.startAnimation();
    });

    effect(() => {
      this.skin();
      this.draw();
    });
  }

  ngOnDestroy(): void {
    this.stopAnimation();
  }

  private startAnimation(): void {
    let tick = 0;
    const loop = () => {
      tick++;
      if (tick % 24 === 0) {
        this.frame = this.frame === 0 ? 1 : 0;
        this.draw();
      }
      this.animId = requestAnimationFrame(loop);
    };
    this.animId = requestAnimationFrame(loop);
  }

  private stopAnimation(): void {
    if (this.animId !== null) {
      cancelAnimationFrame(this.animId);
      this.animId = null;
    }
  }

  private draw(): void {
    const canvasEl = this.canvas()?.nativeElement;
    const shadowEl = this.shadowCanvas()?.nativeElement;
    if (!canvasEl || !shadowEl) { return; }

    const skin = this.skin();
    const config: TrainerSpriteConfig = {
      hatColor: skin.hatColor,
      shirtColor: skin.shirtColor,
      pantsColor: skin.pantsColor,
      skinTone: skin.skinTone,
      character: skin.character,
    };
    const palette = buildPalette(config);
    const srcCanvas = generateTrainerSprite(palette, this.frame, skin.character ?? 'ash');

    const ctx = canvasEl.getContext('2d')!;
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, canvasEl.width, canvasEl.height);
    ctx.drawImage(srcCanvas, 0, 0, canvasEl.width, canvasEl.height);

    const shCtx = shadowEl.getContext('2d')!;
    shCtx.clearRect(0, 0, shadowEl.width, shadowEl.height);
    const grad = shCtx.createRadialGradient(
      shadowEl.width / 2, shadowEl.height / 2, 0,
      shadowEl.width / 2, shadowEl.height / 2, shadowEl.width / 2,
    );
    grad.addColorStop(0, 'rgba(0,0,0,0.35)');
    grad.addColorStop(0.7, 'rgba(0,0,0,0.12)');
    grad.addColorStop(1, 'rgba(0,0,0,0)');
    shCtx.fillStyle = grad;
    shCtx.beginPath();
    shCtx.ellipse(
      shadowEl.width / 2, shadowEl.height / 2,
      shadowEl.width / 2, shadowEl.height / 3,
      0, 0, Math.PI * 2,
    );
    shCtx.fill();
  }
}
