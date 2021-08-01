// SPDX-FileCopyrightText: 2019-2021 Matus Faro <matus@smotana.com>
// SPDX-License-Identifier: AGPL-3.0-only
import { createStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import classNames from 'classnames';
import React, { Component } from 'react';

const styles = (theme: Theme) => createStyles({
  container: {
    display: 'block',
    position: 'relative',
    height: 0,
    overflow: 'hidden',
    margin: 'auto',
  },
  imageAspectRatio: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    maxWidth: '100%',
    maxHeight: '100%',
    transform: 'translate(-50%,-50%)',
  },
});
export interface Props {
  alt: string;
  className?: string;
  imgClassName?: string;
  src: string;
  height?: number | string;
  width?: number | string;
  maxHeight?: number;
  maxWidth?: number;
  styleOuter?: React.CSSProperties;
  style?: React.CSSProperties;
  aspectRatio?: number;
  scale?: number;
  imgProps?: Object;
}
class ImgIso extends Component<Props & WithStyles<typeof styles, true>> {
  render() {
    const scale = this.props.scale || 1;
    var img = (
      <img
        alt={this.props.alt}
        className={classNames(this.props.imgClassName, !!this.props.aspectRatio && this.props.classes.imageAspectRatio)}
        src={this.props.src}
        height={this.props.height}
        width={this.props.width}
        style={this.props.style}
        {...this.props.imgProps}
      />
    );
    if (this.props.aspectRatio) img = (
      <div
        className={this.props.className}
        style={{
          ...this.props.styleOuter,
        }}
      >
        <div
          className={this.props.classes.container}
          style={{
            paddingBottom: !!this.props.maxHeight
              ? `min(${this.props.maxHeight * scale}px, ${100 / this.props.aspectRatio}%)`
              : `${100 / this.props.aspectRatio}%`,
            maxWidth: this.props.maxWidth ? this.props.maxWidth * scale : undefined,
            maxHeight: this.props.maxHeight ? this.props.maxHeight * scale : undefined,
          }}
        >
          {img}
        </div>
      </div>
    );
    return img;
  }
}

export default withStyles(styles, { withTheme: true })(ImgIso);
