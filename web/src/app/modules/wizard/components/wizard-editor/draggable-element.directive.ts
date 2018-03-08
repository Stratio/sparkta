/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved.
 *
 * This software – including all its source code – contains proprietary information of Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled, without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
import { Directive, Output, EventEmitter, AfterContentInit, ElementRef, OnInit, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import * as d3 from 'd3';

import * as wizardActions from 'actions/wizard';
import * as fromRoot from 'reducers';
import { WizardNodePosition } from './../../models/node';

@Directive({ selector: '[svg-draggable]' })
export class DraggableSvgDirective implements AfterContentInit, OnInit {

    @Input() position: WizardNodePosition;

    @Output() positionChange = new EventEmitter<WizardNodePosition>();
    @Output() onClickEvent = new EventEmitter();
    @Output() onDoubleClickEvent = new EventEmitter();

    private clicks = 0;
    private element: d3.Selection<HTMLElement, any, any, any>;
    private lastUpdateCall: number;

    ngOnInit(): void {
        this.setPosition();
    }

    ngAfterContentInit() {
        this.element
            .on('click', this.onClick.bind(this))
            .call(d3.drag()
                .on('drag', this.dragmove.bind(this))
                .on('start', () => {
                    // set wizard state dirty (enable save button)
                    this.store.dispatch(new wizardActions.SetWizardStateDirtyAction());
                }));
    }

    dragmove() {
        const event = d3.event;
        this.position = {
            x: this.position.x + event.dx,
            y: this.position.y + event.dy
        };
        if (this.lastUpdateCall) {
            cancelAnimationFrame(this.lastUpdateCall);
            this.lastUpdateCall = null;
        }
        this.positionChange.emit(this.position);
        this.lastUpdateCall = requestAnimationFrame(this.setPosition.bind(this));
    }

    setPosition() {
        const value = `translate(${this.position.x},${this.position.y})`;
        this.element.attr('transform', value);
    }

    onClick() {
        d3.event.stopPropagation();
        this.clicks++;
        if (this.clicks === 1) {
            setTimeout(() => {
                if (this.clicks === 1) {
                    // single click
                    this.onClickEvent.emit();
                } else {
                    // double click
                    this.onDoubleClickEvent.emit();
                }
                this.clicks = 0;
            }, 200);
        }
    }


    constructor(private elementRef: ElementRef, private store: Store<fromRoot.State>) {
        this.element = d3.select(this.elementRef.nativeElement);
    }
}
