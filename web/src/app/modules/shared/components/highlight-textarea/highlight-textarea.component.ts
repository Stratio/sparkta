///
/// Copyright (C) 2015 Stratio (http://stratio.com)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input,
  OnChanges, OnDestroy, OnInit, ViewChildren, ViewChild, ElementRef, Renderer
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';
import * as CodeMirror from 'codemirror/lib/codemirror';
import * as jsmirror from 'codemirror/mode/javascript/javascript';
import * as sqlmirror from 'codemirror/mode/sql/sql';

import { SpTextareaError } from '@app/shared/components/sp-textarea/sp-textarea.error.model';

@Component({
  selector: 'sp-textarea-highlight',
  templateUrl: './highlight-textarea.template.html',
  styleUrls: ['./highlight-textarea.styles.scss'],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SpHighlightTextareaComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => SpHighlightTextareaComponent), multi: true }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class SpHighlightTextareaComponent implements ControlValueAccessor, OnChanges, OnInit, OnDestroy {
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
  @Input() rows: number = 3;

  /** @Input {string} [wrap='soft'] Define type of wrap as html standard */
  @Input() wrap: string = 'soft';

  /** @Input {object} */
  @Input() config: any;

  @Input() contentType: string;

  @ViewChildren('textarea') vc: any;
  @ViewChild('textarea') host: any;

  public isDisabled: boolean = false; // To check disable
  public focus: boolean = false;
  public errorMessage: string = undefined;
  public instance: any;
  public internalControl: FormControl;
  public internalTextareaModel = '';

  private sub: Subscription;
  private valueChangeSub: Subscription;

  constructor(private _cd: ChangeDetectorRef, private elementRef: ElementRef, private renderer: Renderer) { }

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
    const a = jsmirror;
    const b = sqlmirror;
    this.internalControl = new FormControl(this.internalTextareaModel);
    this.config = this.getConfig(this.contentType);
  }

  getConfig(type: string) {
    switch (type) {
      case 'SQL':
        return {
          mode: 'text/x-hive',
          indentWithTabs: true,
          smartIndent: true,
          lineNumbers: false,
          matchBrackets: true,
          extraKeys: { 'Ctrl-Space': 'autocomplete' }
        };
      case 'JSON': {
        return {
          matchBrackets: true,
          autoCloseBrackets: true,
          mode: 'application/ld+json',
          viewportMargin: Infinity,
          lineWrapping: true
        };
      }
    }
  }


  codemirrorInit(config: any) {
    this.instance = CodeMirror.fromTextArea(this.host.nativeElement, config);
    //this.instance.setValue(this._value);

    this.instance.on('change', () => {
      const value = this.instance.getValue();
      this.internalControl.setValue(value);
      this.onChange(value);
    });

    this.renderer.setElementStyle(this.elementRef.nativeElement.querySelector('.CodeMirror-scroll'), 'min-height', 22 * this.rows + 'px');
  }

  ngAfterViewInit(): void {
    if (this.isFocused) {
      this.focus = true;
      this.vc.first.nativeElement.focus();
    }
    this.codemirrorInit(this.config);

    this.instance.setValue(this.internalControl.value ? this.internalControl.value : '');
  }

  ngOnDestroy(): void {
    if (this.valueChangeSub) {
      this.valueChangeSub.unsubscribe();
    }
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }

  // When value is received from outside
  writeValue(value: any): void {
    if(!value) {
      value = '';
    }
    if (typeof value === 'object') {
      value = JSON.stringify(value, null, 4);
    }
    this.internalControl.setValue(value);
    this.instance && this.instance.setValue(value ? value : '');

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
    if (this.isDisabled) {
      this.instance.setOption("readOnly", true);
    } else {
      this.instance.setOption("readOnly", false);
    }
    this._cd.markForCheck();
  }

  showError(): boolean {
    return this.errorMessage !== undefined && (this.forceValidations) && !this.focus && !this.isDisabled;
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
