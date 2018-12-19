/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import {
   ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input,
   OnChanges, OnDestroy, OnInit, ViewChildren
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subscription } from 'rxjs';

import { SpTextareaError } from './sp-textarea.error.model';

/**
 * @description {Component} [Textarea]
 *
 * The textarea component is for use normally inside a form, you can use too outside a form like a template driven form.
 *
 * @example
 *
 * {html}
 *
 * ```
 * <sp-textarea
 *    label="Components"
 *    placeholder="Number of components"
 *    [forceValidations]="forceValidations"
 *    [errors]="errorsTextarea"
 *    name="components-template"
 *    fieldType="number"
 *    qaTag="components-textarea-template"
 *    required
 *    [(ngModel)]="model.components"
 *    contextualHelp="This is the contextual help of the components"
 *    [cols]="50" [rows]="10">
 * </sp-textarea>
 * ```
 *
 */
@Component({
   selector: 'sp-textarea',
   templateUrl: './sp-textarea.component.html',
   styleUrls: ['./sp-textarea.component.scss'],
   providers: [
      { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SpTextareaComponent), multi: true },
      { provide: NG_VALIDATORS, useExisting: forwardRef(() => SpTextareaComponent), multi: true }
   ],
   changeDetection: ChangeDetectionStrategy.OnPush
})

export class SpTextareaComponent implements ControlValueAccessor, OnChanges, OnInit, OnDestroy {
   /** @Input {string} [placeholder=''] The text that appears as placeholder of the textarea. It is empty by default */
   @Input() placeholder: string = '';

   /** @Input {string} [name=''] Name of the textarea */
   @Input() name: string = '';

   /** @Input {string} [label=''] Label to show over the textarea. It is empty by default */
   @Input() label: string = '';

   /** @Input {StTextareaError} [errors=''] Error to show for each error case, if you don\'t provide this parameter,
    * the default behaviour is only to change color without message
    */
   @Input() errors: SpTextareaError;

   /** @Input {string} [qaTag=''] Id for QA test */
   @Input() qaTag: string;

   /** @Input {boolean} [forceValidations=false] If you specify it to 'true', the textarea checks the errors before being modified by user */
   @Input() forceValidations: boolean = false;

   /** @Input {string} [contextualHelp=''] It will be displayed when user clicks on the info button */
   @Input() contextualHelp: string;

   /** @Input {string} [maxLength=''] Define a max-length for textarea field */
   @Input() maxLength: number;

   /** @Input {boolean} [isFocused=false] If true, the textarea will be focused on view init. */
   @Input() isFocused: boolean = false;

   /** @Input {number} [cols=''] Define textarea number of cols */
   @Input() cols: number;

   /** @Input {number} [rows=''] Define textarea number of rows */
   @Input() rows: number;

   /** @Input {string} [wrap='soft'] Define type of wrap as html standard */
   @Input() wrap: string = 'soft';

   @ViewChildren('textarea') vc: any;

   public isDisabled: boolean = false; // To check disable
   public focus: boolean = false;
   public internalControl: FormControl;
   public errorMessage: string = undefined;
   public pristine = true;

   private sub: Subscription;
   private valueChangeSub: Subscription;
   private internalTextareaModel: any = '';

   constructor(private _cd: ChangeDetectorRef) { }

   onChange = (_: any) => { };
   onTouched = () => { };

   validate(control: FormControl): any {
      if (this.sub) {
         this.sub.unsubscribe();
      }
      this.sub = control.statusChanges.subscribe(() => this.checkErrors(control));
   }

   ngOnChanges(change: any): void {
      if (this.forceValidations && this.internalControl) {
         this.writeValue(this.internalControl.value);
      }
      this._cd.markForCheck();
   }

   ngOnInit(): void {
      this.internalControl = new FormControl(this.internalTextareaModel);
   }

   ngAfterViewInit(): void {
      if (this.isFocused) {
         this.focus = true;
         this.vc.first.nativeElement.focus();
      }
      if (this.forceValidations) {
         this.onChange(this.internalControl.value);
      }
   }

   ngOnDestroy(): void {
      if (this.valueChangeSub) {
         this.valueChangeSub.unsubscribe();
      }
      if (this.sub) {
         this.sub.unsubscribe();
      }
   }

   valuechange(event) {
      this.pristine = false;
      this.onChange(this.internalControl.value);
   }

   // When value is received from outside
   writeValue(value: any): void {
      if (typeof value === 'object') {
         value = JSON.stringify(value, null, 4);
      }
      if (this.forceValidations) {
         this.onChange(value);
      }
      this.internalControl.setValue(value);
      this.internalTextareaModel = value;
   }

   // Registry the change function to propagate internal model changes
   registerOnChange(fn: (_: any) => void): void {
      this.onChange = fn;
   }

   // Registry the touch function to propagate internal touch events TODO: make this function.
   registerOnTouched(fn: () => void): void {
      this.onTouched = fn;
   }

   setDisabledState(disable: boolean): void {
      this.isDisabled = disable;
      if (this.isDisabled && this.internalControl && this.internalControl.enabled) {
         this.internalControl.disable();
      } else if (!this.isDisabled && this.internalControl && this.internalControl.disabled) {
         this.internalControl.enable();
      }
      this._cd.markForCheck();
   }

   showError(): boolean {
      return this.errorMessage !== undefined && (!this.pristine || this.forceValidations) && !this.focus && !this.isDisabled;
   }

   /** Style functions */
   onFocus(event: Event): void {
      this.focus = true;
   }

   onFocusOut(event: Event): void {
      this.focus = false;
   }

   // When status change call this function to check if have errors
   private checkErrors(control: FormControl): void {
      let errors: { [key: string]: any } = control.errors;
      this.errorMessage = this.getErrorMessage(errors);
      this._cd.markForCheck();
   }

   // Get error message in function of error list.
   private getErrorMessage(errors: { [key: string]: any }): string {
      if (!errors) {
         return undefined;
      }

      if (!this.errors) {
         return '';
      }

      if (errors.hasOwnProperty('required')) {
         return this.errors.required || this.errors.generic || '';
      }
      return '';
   }
}
