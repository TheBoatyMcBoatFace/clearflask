/// <reference path="../@types/transform-media-imports.d.ts"/>
import { Container, Link as MuiLink, Slider, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@material-ui/core';
import { createStyles, makeStyles, Theme, withStyles, WithStyles } from '@material-ui/core/styles';
import CheckIcon from '@material-ui/icons/Check';
import React, { Component, useState } from 'react';
import ClearFlaskImg from '../../public/img/clearflask-logo.png';
import CannyImg from '../../public/img/landing/compare/canny.png';
import ConfluxImg from '../../public/img/landing/compare/conflux.png';
import ConvasImg from '../../public/img/landing/compare/convas.png';
import FeatureUpvoteImg from '../../public/img/landing/compare/featureupvote.png';
import FiderImg from '../../public/img/landing/compare/fider.png';
import HelloNextImg from '../../public/img/landing/compare/hellonext.png';
import NoltImg from '../../public/img/landing/compare/nolt.png';
import NooraImg from '../../public/img/landing/compare/noora.png';
import RoadmapSpaceImg from '../../public/img/landing/compare/roadmapspace.png';
import SuggestedImg from '../../public/img/landing/compare/suggested.png';
import UpvotyImg from '../../public/img/landing/compare/upvoty.png';
import UserVoiceImg from '../../public/img/landing/compare/uservoice.png';
import ImgIso from '../common/ImgIso';
import { notEmpty } from '../common/util/arrayUtil';


const PlatformClearFlask = 'clearflask';
const PlatformUserVoice = 'uservoice';
const PlatformCanny = 'canny';
const PlatformUpvoty = 'upvoty';
const PlatformFider = 'fider';
const PlatformHelloNext = 'hellonext';
const PlatformConvas = 'convas';
const PlatformConflux = 'conflux';
const PlatformNolt = 'nolt';
const PlatformSuggested = 'suggested';
const PlatformNoora = 'noora';
const PlatformRoadmapSpace = 'roadmapspace';
const PlatformFeatureUpvote = 'featureupvote';

const percTotalUsersAreActive3Months = 0.05;
const percTotalUsersAreActive60Days = 0.05;
const percTotalUsersAreTracked = 0.05;
const euroToUsd = 1.2;

interface PlanPricing {
  basePrice: number;
  baseUsers?: number;
  unitPrice?: number;
  unitUsers?: number;
}
interface Platform {
  id: string;
  name: string;
  logo: Img;
  color: string;
  pricing: Array<PlanPricing>;
}
const Platforms: { [platformId: string]: Platform } = {
  [PlatformClearFlask]: {
    id: PlatformClearFlask,
    name: 'ClearFlask',
    logo: ClearFlaskImg,
    color: '#019c93',
    pricing: [
      { basePrice: 10, baseUsers: 50 / percTotalUsersAreTracked, unitPrice: 10, unitUsers: 50 / percTotalUsersAreTracked },
      { basePrice: 100, baseUsers: 500 / percTotalUsersAreTracked, unitPrice: 50, unitUsers: 500 / percTotalUsersAreTracked },
    ],
  },
  [PlatformUserVoice]: {
    id: PlatformUserVoice,
    name: 'UserVoice',
    logo: UserVoiceImg,
    color: 'rgb(237,112,56)',
    pricing: [{ basePrice: 500 }],
  },
  [PlatformCanny]: {
    id: PlatformCanny,
    name: 'Canny',
    logo: CannyImg,
    color: 'rgb(94,98,240)',
    pricing: [
      { basePrice: 50, baseUsers: 100 / percTotalUsersAreTracked, unitPrice: 20, unitUsers: 100 / percTotalUsersAreTracked },
      { basePrice: 200, baseUsers: 1000 / percTotalUsersAreTracked, unitPrice: 100, unitUsers: 1000 / percTotalUsersAreTracked },
    ],
  },
  [PlatformUpvoty]: {
    id: PlatformUpvoty,
    name: 'Upvoty',
    logo: UpvotyImg,
    color: 'rgb(233,134,85)',
    pricing: [
      { basePrice: 15, baseUsers: 150 / percTotalUsersAreTracked },
      { basePrice: 25, baseUsers: 1500 / percTotalUsersAreTracked },
      { basePrice: 49 },
    ],
  },
  [PlatformFider]: {
    id: PlatformFider,
    name: 'Fider',
    logo: FiderImg,
    color: 'rgb(0,0,0)',
    pricing: [{ basePrice: 0 }],
  },
  [PlatformHelloNext]: {
    id: PlatformHelloNext,
    name: 'HelloNext',
    logo: HelloNextImg,
    color: 'rgb(10,73,176)',
    pricing: [{ basePrice: 50 }],
  },
  [PlatformConvas]: {
    id: PlatformConvas,
    name: 'Convas',
    logo: ConvasImg,
    color: 'rgb(72,166,248)',
    pricing: [
      { basePrice: 0, baseUsers: 50 / percTotalUsersAreTracked },
      { basePrice: 15, baseUsers: 100 / percTotalUsersAreTracked, unitPrice: 15, unitUsers: 100 / percTotalUsersAreTracked },
      { basePrice: 150 },
    ],
  },
  [PlatformConflux]: {
    id: PlatformConflux,
    name: 'Conflux',
    logo: ConfluxImg,
    color: 'rgb(81,183,249)',
    pricing: [
      { basePrice: 10 * euroToUsd, baseUsers: 50 / percTotalUsersAreActive3Months },
      { basePrice: 50 * euroToUsd, baseUsers: 250 / percTotalUsersAreActive3Months },
      { basePrice: 80 * euroToUsd, baseUsers: 500 / percTotalUsersAreActive3Months },
      { basePrice: 130 * euroToUsd, baseUsers: 1000 / percTotalUsersAreActive3Months },
    ],
  },
  [PlatformNolt]: {
    id: PlatformNolt,
    name: 'Nolt',
    logo: NoltImg,
    color: 'rgb(237,106,102)',
    pricing: [{ basePrice: 25 }],
  },
  [PlatformSuggested]: {
    id: PlatformSuggested,
    name: 'Suggested',
    logo: SuggestedImg,
    color: '#286ffa',
    pricing: [{ basePrice: 50 }],
  },
  [PlatformNoora]: {
    id: PlatformNoora,
    name: 'Noora',
    logo: NooraImg,
    color: 'rgb(234,53,117)',
    pricing: [{ basePrice: 60 }],
  },
  [PlatformRoadmapSpace]: {
    id: PlatformRoadmapSpace,
    name: 'RoadmapSpace',
    logo: RoadmapSpaceImg,
    color: 'rgb(153,123,247)',
    pricing: [
      { basePrice: 29, baseUsers: 500 / percTotalUsersAreActive60Days },
      { basePrice: 49, baseUsers: 1500 / percTotalUsersAreActive60Days },
      { basePrice: 99, baseUsers: 3000 / percTotalUsersAreActive60Days },
    ],
  },
  [PlatformFeatureUpvote]: {
    id: PlatformFeatureUpvote,
    name: 'FeatureUpvote',
    logo: FeatureUpvoteImg,
    color: 'rgb(50,58,70)',
    pricing: [{ basePrice: 99 }],
  },
};

const styles = (theme: Theme) => createStyles({
  emphasize: {
    fontWeight: 'bolder',
  },
  heading: {
    marginTop: theme.spacing(6),
  },
  tableHeading: {
    textAlign: 'center',
  },
  check: {
    margin: 'auto',
    display: 'block',
    color: theme.palette.primary.main,
  },
  table: {
    width: 'min-content',
  },
  sliderValueHorizontal: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'baseline',
  },
  sliderFloatingInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    position: 'relative',
    transition: theme.transitions.create(['bottom'], {
      duration: theme.transitions.duration.shortest,
      easing: theme.transitions.easing.easeOut,
    }),
    transform: 'translateY(50%)',
    flex: '1',
    overflow: 'visible',
  },
  sliderOuterContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: 250,
    width: 250,
  },
  sliderContainer: {
    flex: '1',
    height: '100%',
    width: '100%',
    position: 'relative',
    display: 'flex',
    alignItems: 'flex-end',
    justifyContent: 'center',
    padding: theme.spacing(4, 0),
  },
  brandContainer: {
    display: 'inline-flex',
    alignItems: 'center',
  },
  brandImg: {
    width: 14,
    marginRight: theme.spacing(1),
  },
  brandName: {
    display: 'inline-block',
  },
  pricingContainer: {
    display: 'flex',
    flexWrap: 'wrap',
  },
  priceContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'baseline',
  },
});
const useStyles = makeStyles(styles);
interface State {
}
class Competitors extends Component<WithStyles<typeof styles, true>, State> {
  state: State = {};

  render() {
    return (
      <Container maxWidth='md'>
        <Intro />
        <Volume />
        <PricingCompare />
        <MajorFeatures />
        <VotingMechanism />
        <Onboarding />
      </Container>
    );
  }
}
const Intro = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>Problem outline</Typography>
      <p><Typography>You’ve created a product and you want to know which area you should focus on next. A great source of ideas is your customer voice, but the thought of going through the process of gathering, summarizing and prioritizing what your customers want is cumbersome.</Typography></p>
      <p><Typography>This is where customer feedback platforms can help. Most are a drop-in platform you can integrate with your website, app or product to start collecting feedback right away. The right tool will not only help in de-duplicating and combining ideas together, but also prioritizing feedback based on customer value or other factors important to you.</Typography></p>
      <p><Typography>An important aspect of feedback collection is to make your customer base feel heard. Keeping your product development in touch with your customers will help your reputation and also keep your customers engaged. Your customer will love to hear from you when you release their long-awaited feature.</Typography></p>
      <p><Typography>So which feedback tool is right for you?</Typography></p>
    </React.Fragment>
  );
};

const Volume = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>How many users do you expect?</Typography>
      <p><Typography>The amount of feedback you will receive depends on your current or predicted number of customers and how tightly you integrate the feedback tool with your product.</Typography></p>
      <p><Typography>The <span className={classes.emphasize}>coverage rate</span> metric measures the percentage of users that have provided you with feedback by posting a unique idea or indicating that an existing idea is suited for them. UserVoice <ExternalLink url='https://community.uservoice.com/blog/new-in-uservoice-coverage-rate-reporting/'>reports</ExternalLink> that at least 15% coverage rate is considered satisfactory with typical rate ranging up to 50%. While Canny customers see a typical rate of 5%.</Typography></p>
      <p><Typography>Some platforms are not only suited to handle large amounts of feedback, but do not provide the tools to organize and sort through that feedback afterwards. Typically you will fall under one of these categories:</Typography></p>

      <Typography component='h3' variant='h5' className={classes.heading}>High Volume</Typography>
      <Typography variant='subtitle1'>Enterprise or proven product</Typography>
      <p><Typography>If you have a clear cut use case that requires a high volume of feedback, you need to choose a platform that can handle this amount of feedback. Collecting feedback is one thing, but how do you sort throught it at scale matters.</Typography></p>
      <p><Typography>UserVoice is the market leader in this space with Canny and ClearFlask as alternative options.</Typography></p>

      <Typography component='h3' variant='h5' className={classes.heading}>Variable Volume</Typography>
      <Typography variant='subtitle1'>Startups or growing product</Typography>
      <p><Typography>Startups or products with growth should look at platforms that can handle high-volume and scale with your needs. Beware of unlimited plans that usually indicate the platform has never experienced a high-volume customer and may not be able to handle your future volume.</Typography></p>
      <p><Typography>ClearFlask and Canny are both built for high-volume while only charging you based on your current volume.</Typography></p>

      <Typography component='h3' variant='h5' className={classes.heading}>Low Volume with many projects</Typography>
      <Typography variant='subtitle1'>Serial hobbyist</Typography>
      <p><Typography>If you collect feedback across many small separate projects, you don't want to be paying and maintaining separate accounts and billing for each project.</Typography></p>
      <p><Typography>At ClearFlask, you can have multiple projects under the same account and you only pay for the combined user count across all of your projects. Canvas has a free-tier for projects under 50 users, while Fider is free and open-source for only the cost of hosting and managing your instances.</Typography></p>

      <Typography component='h3' variant='h5' className={classes.heading}>Low Volume</Typography>
      <Typography variant='subtitle1'>Small group, hobbyist</Typography>
      <p><Typography>For collecting feedback from a small group of users, there are many options available. Keep reading below to find the product based on other factors. At least you can eliminate UserVoice as they are not suitable for you.</Typography></p>
    </React.Fragment>
  );
};

const sliderMarks = [
  0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
  12000, 14000, 16000, 18000, 20000,
  30000, 40000, 50000, 60000,
];
const PricingCompare = (props: {}) => {
  const classes = useStyles();
  const [markIndex, setMarkIndex] = useState<number>(5);
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>What is your budget?</Typography>
      <p><Typography>Although pricing shouldn't be your sole factor for determining your solution, you should know how much are you willing to spend. If your product has variable number of users, calculate how much you are willing to spend per customer.</Typography></p>
      <p><Typography>Look at your current or predicted number of total users you have to estimate your price below</Typography></p>

      <p><Typography>TODO add pricing chart here</Typography></p>
      <div className={classes.pricingContainer}>
        <UserCountSlider
          marks={sliderMarks}
          markIndex={markIndex}
          markIndexChanged={setMarkIndex}
          totalUserActiveUserRatio={0.05}
        />
        <PricingTable totalUsers={sliderMarks[markIndex]} />
      </div>

      <Typography variant='caption'>Note: To compare platforms with different definitions of paid users, we estimate tracked and active users to be 5% of your total users.</Typography>
    </React.Fragment>
  );
};
const PricingTable = (props: {
  totalUsers: number;
}) => {
  const classes = useStyles();

  const data: Array<{
    platform: Platform,
    price: number,
  }> = Object.values(Platforms).map(platform => {
    var price: number | undefined;
    for (const plan of platform.pricing) {
      if (plan.baseUsers !== undefined && plan.baseUsers > props.totalUsers) continue;
      var planPrice = plan.basePrice;
      if (plan.unitPrice && plan.unitUsers) {
        var usersCovered = plan.baseUsers || 0;
        while (usersCovered < props.totalUsers) {
          usersCovered += plan.unitUsers;
          planPrice += plan.unitPrice;
        }
      }
      if (price !== undefined && price <= planPrice) {
        break;
      }
      price = planPrice;
    }
    return { platform, price: price || -1 };
  })
    .filter(notEmpty)
    .sort((l, r) => r.price - l.price);

  return (
    <div className={classes.table}>
      <Table size='small'>
        <TableBody>
          {data.map(row => (
            <TableRow key={row.platform.id}>
              <TableCell key='platformName'><Brand platformId={row.platform.id} /></TableCell>
              <TableCell key='price'><Price val={row.price} /></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
const Price = (props: {
  val: number;
}) => {
  const classes = useStyles();
  if (props.val < 0) return (
    <div className={classes.priceContainer}>
      <Typography component='div' variant='h6'>N/A</Typography>
    </div>
  );
  return (
    <div className={classes.priceContainer}>
      <Typography component='div' variant='subtitle2' color='textSecondary' style={{ alignSelf: 'flex-start' }}>{'$'}</Typography>
      <Typography component='div' variant='h5'>{formatNumber(props.val)}</Typography>
      <Typography component='div' variant='subtitle2' color='textSecondary'>/&nbsp;mo</Typography>
    </div>
  );
};

const MajorFeatures = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>Which features matter to you?</Typography>
      <p><Typography>Each platform has a set of major components that are available. Be prepared that although two platforms have the same component, the functionality of that component may drastically differ. Use this table as a quick comparison:</Typography></p>
      <ComparisonTable
        headers={[
          { headingId: 'ideas', content: 'Collect Feedback' },
          { headingId: 'roadmap', content: 'Public Roadmap' },
          { headingId: 'changelog', content: 'Changelog' },
          { headingId: 'knowledge', content: 'Knowledge base' },
          { headingId: 'forum', content: 'Forum' },
          { headingId: 'blog', content: 'Blog' },
          { headingId: 'custom', content: 'Custom' },
        ]}
        data={[
          { platformId: PlatformClearFlask, headingIds: new Set(['ideas', 'roadmap', 'changelog', 'knowledge', 'forum', 'blog', 'custom']) },
          { platformId: PlatformUserVoice, headingIds: new Set(['ideas', 'roadmap', 'changelog', 'knowledge', 'forum']) },
          { platformId: PlatformNoora, headingIds: new Set(['ideas', 'roadmap', 'changelog', 'knowledge']) },
          { platformId: PlatformConflux, headingIds: new Set(['ideas', 'roadmap', 'changelog', 'forum']) },
          { platformId: PlatformCanny, headingIds: new Set(['ideas', 'roadmap', 'changelog']) },
          { platformId: PlatformUpvoty, headingIds: new Set(['ideas', 'roadmap', 'changelog']) },
          { platformId: PlatformHelloNext, headingIds: new Set(['ideas', 'roadmap', 'changelog']) },
          { platformId: PlatformSuggested, headingIds: new Set(['ideas', 'roadmap', 'changelog']) },
          { platformId: PlatformRoadmapSpace, headingIds: new Set(['ideas', 'roadmap', 'changelog']) },
          { platformId: PlatformConvas, headingIds: new Set(['ideas', 'roadmap']) },
          { platformId: PlatformNolt, headingIds: new Set(['ideas', 'roadmap']) },
          { platformId: PlatformFeatureUpvote, headingIds: new Set(['ideas', 'changelog']) },
          { platformId: PlatformFider, headingIds: new Set(['ideas']) },
        ]}
      />
    </React.Fragment>
  );
};

const VotingMechanism = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>Prioritization of feedback</Typography>
      <p><Typography>Most platforms can collect a lot of feedback, while only a few can handle making sense of it all.</Typography></p>
      <p><Typography>To better understand the feedback you collected, you need to consider the value of each of your customers. For example, you may want to know which features your customers wanted considering revenue. Or you may want to see only your recently churned customers in your enterprise plan.</Typography></p>

      <Typography component='h4' variant='h6'>Analyze externally</Typography>
      <p><Typography>One way to accomplish this is to export your data to an external Data Warehouse or analytics tool to further analyze your data which allows you to correlate with other data you have about your customers.</Typography></p>

      <Typography component='h4' variant='h6'>Segmentation</Typography>
      <p><Typography>Some platforms allow you to import arbitrary customer data in order to filter, search and assign weights to your customers.</Typography></p>

      <Typography component='h4' variant='h6'>Credit system</Typography>
      <p><Typography>Another approach is to issue credits to your customers based on their value such as monthly spend. They can then spend their credits on the features they want.</Typography></p>

      <p><Typography>Whether you choose to give control to your users with fine-grained ranking or analyze behind the scenes (or both) is dependant on your specific use case.</Typography></p>
      <ComparisonTable
        headers={[
          { headingId: 'up', content: 'Upvote' },
          { headingId: 'down', content: 'Upvote & Downvote' },
          { headingId: 'emoji', content: 'Weighted Emoji' },
          { headingId: 'credit', content: 'Credit System' },
          { headingId: 'fund', content: 'Crowd-funding' },
          { headingId: 'segment', content: 'Segment' },
          { headingId: 'dw', content: 'External DataWarehouse' },
        ]}
        data={[
          { platformId: PlatformUserVoice, headingIds: new Set(['segment', 'up', 'dw']) },
          { platformId: PlatformCanny, headingIds: new Set(['segment', 'up']) },
          { platformId: PlatformClearFlask, headingIds: new Set(['up', 'down', 'emoji', 'credit', 'fund']) },
          { platformId: PlatformHelloNext, headingIds: new Set(['up', 'down']) },
          { platformId: PlatformNolt, headingIds: new Set(['up', 'down']) },
          { platformId: '...', headingIds: new Set(['up']) },
        ]}
      />
    </React.Fragment>
  );
};

const Onboarding = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}>Engagement channels</Typography>
      <p><Typography>The value of feedback is drastically different between a customer or someone that has no intention in being your customer. One way to ensure that feedback is valuable is to ask the user to provide a communication channel so they can be notified when their feedback is addressed.</Typography></p>

      <Typography component='h4' variant='h6'>Onboarding friction</Typography>
      <p><Typography>Users are hesitant to provide their personal information including their email address. The more personal information you ask during sign-up will result in less feedback you will receive.</Typography></p>
      <p><Typography>If you manage your customer accounts already, <span className={classes.emphasize}>Single Sign-On</span> is the right solution as it allows you to seamlessly login your users in the background with no login screen.</Typography></p>

      <Typography component='h4' variant='h6'>Guest / Anonymous feedback</Typography>
      <p><Typography>Ideal in narrow use cases, allows your users to sign-up without providing any contact information. Use this only as a last resort as it attracts spam and leaves you with no engagement opportunity.</Typography></p>
      <p><Typography><span className={classes.emphasize}>Browser Push Notifications</span> are an alternative where your users don't have to provide their email, but you have a communication channel open.</Typography></p>

      <ComparisonTable
        headers={[
          { headingId: 'email', content: 'Email' },
          { headingId: 'guest', content: 'Guest' },
          { headingId: 'sso', content: 'SSO' },
          { headingId: 'saml', content: 'SAML' },
          { headingId: 'oauth', content: 'OAuth' },
          { headingId: 'browserpush', content: 'Browser Push' },
          { headingId: 'emaildomain', content: 'Email Domain' },
          { headingId: 'openid', content: 'OpenId' },
          { headingId: 'okta', content: 'Okta' },
        ]}
        data={[
          { platformId: PlatformUserVoice, headingIds: new Set(['email', 'emaildomain', 'sso', 'saml', 'okta', 'openid']) },
          { platformId: PlatformClearFlask, headingIds: new Set(['guest', 'browserpush', 'email', 'emaildomain', 'sso', 'oauth']) },
          { platformId: PlatformCanny, headingIds: new Set(['email', 'emaildomain', 'sso']) },
          { platformId: PlatformFider, headingIds: new Set(['email', 'oauth']) },
          { platformId: PlatformFeatureUpvote, headingIds: new Set(['email', 'saml']) },
          { platformId: PlatformUpvoty, headingIds: new Set(['guest', 'email', 'sso']) },
          { platformId: PlatformHelloNext, headingIds: new Set(['guest', 'email', 'sso']) },
          { platformId: PlatformConflux, headingIds: new Set(['guest', 'email', 'sso']) },
          { platformId: PlatformSuggested, headingIds: new Set(['guest', 'email', 'sso']) },
          { platformId: PlatformNolt, headingIds: new Set(['guest', 'email', 'sso']) },
          { platformId: PlatformNoora, headingIds: new Set(['email', 'sso']) },
          { platformId: PlatformConvas, headingIds: new Set(['guest', 'email']) },
          { platformId: PlatformRoadmapSpace, headingIds: new Set(['email']) },
        ]}
      />

      <Typography component='h4' variant='h6' className={classes.heading}>OAuth, SAML</Typography>
      <p><Typography>Although OAuth and SAML allow you to login with the vast majority of external providers including Facebook and Google, some platforms have a built-in shared login for specific providers shown below.</Typography></p>

      <ComparisonTable
        headers={[
          { headingId: 'google', content: 'Google' },
          { headingId: 'fb', content: 'Facebook' },
          { headingId: 'github', content: 'Github' },
          { headingId: 'twitter', content: 'Twitter' },
          { headingId: 'discord', content: 'Discord' },
          { headingId: 'steam', content: 'Steam' },
          { headingId: 'apple', content: 'Apple' },
          { headingId: 'wordpress', content: 'WordPress' },
          { headingId: 'azure', content: 'AzureAD' },
          { headingId: 'gsuite', content: 'GSuite' },
        ]}
        data={[
          { platformId: PlatformCanny, headingIds: new Set(['fb', 'twitter', 'github', 'google', 'azure', 'gsuite']) },
          { platformId: PlatformConflux, headingIds: new Set(['fb', 'google', 'discord', 'steam']) },
          { platformId: PlatformUpvoty, headingIds: new Set(['twitter', 'google', 'wordpress']) },
          { platformId: PlatformFider, headingIds: new Set(['fb', 'github', 'google']) },
          { platformId: PlatformHelloNext, headingIds: new Set(['github', 'google', 'apple']) },
          { platformId: PlatformSuggested, headingIds: new Set(['fb', 'github', 'google']) },
          { platformId: PlatformNolt, headingIds: new Set(['twitter', 'google']) },
          { platformId: PlatformUserVoice, headingIds: new Set(['fb', 'google']) },
          { platformId: PlatformNoora, headingIds: new Set(['google']) },
        ]}
      />
    </React.Fragment>
  );
};

const TemplateDeleteMe = (props: {}) => {
  const classes = useStyles();
  return (
    <React.Fragment>
      <Typography component='h2' variant='h4' className={classes.heading}></Typography>
      <p><Typography></Typography></p>
    </React.Fragment>
  );
};

const ComparisonTable = (props: {
  headers: Array<{
    headingId: string;
    content: React.ReactNode;
  }>;
  data: Array<{
    platformId: string;
    headingIds: Set<string>;
  }>;
}) => {
  const classes = useStyles();
  return (
    <div className={classes.table}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell key='platformName'></TableCell>
            {props.headers.map(header => (
              <TableCell key={header.headingId} className={classes.tableHeading}>{header.content}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {props.data.map(row => (
            <TableRow key={row.platformId}>
              <TableCell key='platformName'><Brand platformId={row.platformId} /></TableCell>
              {props.headers.map(header => (
                <TableCell key={header.headingId}>
                  {row.headingIds.has(header.headingId) ? (
                    <CheckIcon titleAccess='Yes' color='inherit' fontSize='inherit' className={classes.check} />
                  ) : (
                    null
                  )}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};


const formatNumber = (val: number): string => {
  return val.toLocaleString('en-US');
}

const UserCountSlider = (props: {
  marks: Array<number>;
  totalUserActiveUserRatio: number;
  markIndex: number;
  markIndexChanged: (index: number) => void;
}) => {
  const classes = useStyles();
  const min = 0;
  const max = props.marks.length - 1;
  const bottom = `${props.markIndex / (max - min) * 100}%`;
  return (
    <div className={classes.sliderOuterContainer}>
      <div className={classes.sliderContainer}>
        <div className={classes.sliderFloatingInfo} style={{ bottom }}>
          <div className={classes.sliderValueHorizontal}>
            <Typography variant='h6' component='div'>{formatNumber(props.marks[props.markIndex])}</Typography>
          </div>
          <div className={classes.sliderValueHorizontal}>
            <Typography variant='caption' component='div'>Total users</Typography>
          </div>
        </div>
        <Slider
          value={props.markIndex}
          min={min}
          step={1}
          orientation='vertical'
          max={max}
          onChange={(e, val) => props.markIndexChanged(val as any as number)}
        />
        <div className={classes.sliderFloatingInfo} style={{ bottom }}>
          <div className={classes.sliderValueHorizontal}>
            <Typography variant='h6' component='div'>{formatNumber(props.marks[props.markIndex] * props.totalUserActiveUserRatio)}</Typography>
          </div>
          <div className={classes.sliderValueHorizontal}>
            <Typography variant='caption' component='div'>Active users</Typography>
          </div>
        </div>
      </div>
    </div>
  );
};

const Brand = (props: ({ platformId: string; } | { platform: Platform }) & {
}) => {
  const classes = useStyles();

  const platform: Platform | undefined = props['platform'] || Platforms[props['platformId']]
  if (!platform) return props['platformId'];

  return (
    <div className={classes.brandContainer}>
      <ImgIso
        className={classes.brandImg}
        alt={platform.name}
        src={platform.logo.src}
        aspectRatio={platform.logo.aspectRatio}
        maxWidth={platform.logo.width}
        maxHeight={platform.logo.height}
      />
      <Typography color='inherit' style={{ color: platform.color }}>
        {platform.name}
      </Typography>
    </div>
  );
};

const ExternalLink = (props: {
  url: string;
  children: React.ReactNode;
}) => {
  const classes = useStyles();
  return (
    <MuiLink
      href={props.url}
      target='_blank'
      underline='none'
      rel='noopener nofollow'
    >
      {props.children}
    </MuiLink>
  );
};

export default withStyles(styles, { withTheme: true })(Competitors);
