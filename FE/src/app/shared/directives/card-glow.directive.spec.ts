import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CardGlowDirective } from './card-glow.directive';

@Component({
  template: `<div appCardGlow class="card" style="width: 100px; height: 140px;"></div>`,
  imports: [CardGlowDirective],
})
class HostComponent {}

describe('CardGlowDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let element: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HostComponent],
    });

    fixture = TestBed.createComponent(HostComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    element = fixture.debugElement.query(By.directive(CardGlowDirective)).nativeElement;
  });

  afterEach(() => {
    fixture.nativeElement.remove();
  });

  it('sets --glow-x and --glow-y based on cursor position on mousemove', () => {
    const rect = element.getBoundingClientRect();
    const event = new MouseEvent('mousemove', {
      clientX: rect.left + rect.width / 4,
      clientY: rect.top + rect.height / 2,
    });

    element.dispatchEvent(event);
    fixture.detectChanges();

    expect(element.style.getPropertyValue('--glow-x')).not.toBe('');
    expect(element.style.getPropertyValue('--glow-y')).not.toBe('');
  });

  it('resets --glow-x and --glow-y to 50% on mouseleave', () => {
    element.dispatchEvent(new MouseEvent('mousemove', { clientX: 10, clientY: 10 }));
    fixture.detectChanges();

    element.dispatchEvent(new MouseEvent('mouseleave'));
    fixture.detectChanges();

    expect(element.style.getPropertyValue('--glow-x')).toBe('50%');
    expect(element.style.getPropertyValue('--glow-y')).toBe('50%');
  });
});
