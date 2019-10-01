
import React, { PureComponent } from 'react';
import {requireNativeComponent, NativeModules, UIManager, findNodeHandle } from 'react-native';
import PropTypes from 'prop-types';

requireNativeComponent
export default class FilterView extends PureComponent {

    constructor( props ) {
        super( props );
        this.saturation = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setSaturation
        this.contrast = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setContrast
        this.brightness = UIManager.getViewManagerConfig( 'RNFilter' ).Commands.setBrightness
        this.setSaturation = this._setSaturation.bind( this )
        this.setContrast = this._setContrast.bind( this )
        this.setBrightness = this._setBrightness.bind( this )
    }

    componentDidMount() {
        this.props.onRef && this.props.onRef(this)
        this.viewHandle = findNodeHandle(this.filterRef);
    }


    _setSaturation( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.saturation,
            [ value ]
        );
    }
    
    _setBrightness( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.brightness,
            [ value ]
        );
    }
    
    _setContrast( value ) {
        UIManager.dispatchViewManagerCommand(
            this.viewHandle,
            this.contrast,
            [ value ]
        );
    }
    
    render() {
        return (
            <RNCamera
                style={ this.props.style }
                src={this.props.src}
                ref={ref => this.filterRef = ref}
                onDataReturned={this.onDataReturned}
            />
        )
    }
}

const RNCamera = requireNativeComponent("RNFilter", FilterView, {
    nativeOnly: { onDataReturned: true  }
  });