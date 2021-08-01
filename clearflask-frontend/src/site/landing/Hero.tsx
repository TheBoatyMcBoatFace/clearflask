// SPDX-FileCopyrightText: 2019-2021 Matus Faro <matus@smotana.com>
// SPDX-License-Identifier: AGPL-3.0-only
import { Button, Grid, Link as MuiLink, Typography } from '@material-ui/core';
import { createStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import ImgIso from '../../common/ImgIso';
import Vidyard from '../../common/Vidyard';

const styles = (theme: Theme) => createStyles({
  hero: {
    width: '100vw',
    minHeight: theme.vh(40),
    padding: `${theme.vh(10)}px 10vw`,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
  },
  heroTextContainer: {
    display: 'flex',
    flexDirection: 'column',
  },
  heroDescription: {
    marginTop: theme.spacing(2),
    color: theme.palette.text.secondary,
  },
  image: {
    width: '100%',
    [theme.breakpoints.up('md')]: {
      padding: theme.spacing(8),
    },
    [theme.breakpoints.down('sm')]: {
      padding: theme.spacing(8, 2),
    },
  },
  buttonAndRemark: {
    alignSelf: 'flex-end',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    margin: theme.spacing(4, 10),
    [theme.breakpoints.down('sm')]: {
      margin: theme.spacing(4, 6),
    },
  },
  remark: {
    textAlign: 'center',
    margin: theme.spacing(1),
  },
});

interface Props {
  title?: string;
  description?: string | React.ReactNode;
  image?: Img;
  imagePath?: string;
  vidyard?: {
    image: Img;
    uuid: string;
  };
  mirror?: boolean;
  buttonTitle?: string;
  buttonLinkExt?: string;
  buttonLink?: string;
  buttonRemark?: React.ReactNode;
}
class Hero extends Component<Props & WithStyles<typeof styles, true>> {

  render() {
    const imageSrc = this.props.image?.src || this.props.imagePath;
    const media = imageSrc ? (
      <ImgIso
        alt=''
        className={this.props.classes.image}
        src={imageSrc}
        aspectRatio={this.props.image?.aspectRatio}
        width={!this.props.image?.aspectRatio ? '100%' : undefined}
        maxWidth={this.props.image?.width}
        maxHeight={this.props.image?.height}
      />
    ) : (this.props.vidyard ? (
      <Vidyard
        className={this.props.classes.image}
        {...this.props.vidyard}
      />
    ) : undefined);
    return (
      <div className={this.props.classes.hero}>
        <Grid container
          justify='center'
          wrap='wrap-reverse'
          alignItems='center'
          direction={!!this.props.mirror ? 'row-reverse' : undefined}
        >
          {media && (
            <Grid item xs={12} md={6}>
              {media}
            </Grid>
          )}
          <Grid item xs={12} md={6} lg={5} xl={4} className={this.props.classes.heroTextContainer}>
            <Typography variant='h3' component='h1'>
              {this.props.title}
            </Typography>
            <Typography variant='h5' component='h2' className={this.props.classes.heroDescription}>
              {this.props.description}
            </Typography>
            {this.props.buttonTitle && (
              <div
                className={this.props.classes.buttonAndRemark}
              >
                <Button
                  color='secondary'
                  variant='contained'
                  disableElevation
                  style={{ fontWeight: 900, color: 'white', }}
                  {...(this.props.buttonLink ? {
                    component: Link,
                    to: this.props.buttonLink,
                  } : {})}
                  {...(this.props.buttonLinkExt ? {
                    component: MuiLink,
                    href: this.props.buttonLinkExt,
                  } : {})}
                >
                  {this.props.buttonTitle}
                </Button>
                {!!this.props.buttonRemark && (
                  <div className={this.props.classes.remark}>
                    <Typography variant='caption' component='div' color='textSecondary'>{this.props.buttonRemark}</Typography>
                  </div>
                )}
              </div>
            )}
          </Grid>
        </Grid>
      </div>
    );
  }
}

export default withStyles(styles, { withTheme: true })(Hero);
